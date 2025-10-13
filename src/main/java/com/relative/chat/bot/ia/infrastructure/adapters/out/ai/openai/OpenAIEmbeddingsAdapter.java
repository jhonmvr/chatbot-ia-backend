package com.relative.chat.bot.ia.infrastructure.adapters.out.ai.openai;

import com.relative.chat.bot.ia.application.ports.out.EmbeddingsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.SocketException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter para generar embeddings usando OpenAI Embeddings API
 * 
 * Documentación: https://platform.openai.com/docs/guides/embeddings
 * API Reference: https://platform.openai.com/docs/api-reference/embeddings
 * 
 * Modelos soportados:
 * - text-embedding-3-large (3072 dimensiones, mejor calidad)
 * - text-embedding-3-small (1536 dimensiones, más económico)
 * - text-embedding-ada-002 (1536 dimensiones, legacy)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.embeddings.provider", havingValue = "openai", matchIfMissing = false)
public class OpenAIEmbeddingsAdapter implements EmbeddingsPort {
    
    private final WebClient openAiWebClient;
    
    @Value("${app.ai.openai.embeddings.model:text-embedding-3-large}")
    private String model;
    
    @Value("${app.ai.openai.embeddings.dimensions:3072}")
    private Integer dimensions;
    
    @Value("${app.ai.openai.api-key}")
    private String apiKey;
    
    @Value("${app.ai.openai.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${app.ai.openai.retry.min-backoff:1}")
    private long minBackoffSeconds;
    
    @Value("${app.ai.openai.retry.max-backoff:10}")
    private long maxBackoffSeconds;
    
    /**
     * Request para OpenAI Embeddings API
     * 
     * @param input Texto o lista de textos (max 8191 tokens cada uno)
     * @param model Modelo de embeddings
     * @param dimensions Número de dimensiones (opcional, solo para text-embedding-3-*)
     * @param encodingFormat Formato (float o base64)
     * @param user ID de usuario para tracking (opcional)
     */
    record EmbeddingsRequest(
        Object input,              // String o List<String>
        String model,
        Integer dimensions,        // Solo para text-embedding-3-*
        String encoding_format,    // "float" o "base64"
        String user                // Opcional
    ) {
        // Constructor para un solo texto
        static EmbeddingsRequest single(String text, String model, Integer dimensions) {
            return new EmbeddingsRequest(text, model, dimensions, "float", null);
        }
        
        // Constructor para múltiples textos
        static EmbeddingsRequest batch(List<String> texts, String model, Integer dimensions) {
            return new EmbeddingsRequest(texts, model, dimensions, "float", null);
        }
    }
    
    /**
     * Response de OpenAI Embeddings API
     */
    record EmbeddingsResponse(
        String object,             // "list"
        List<EmbeddingData> data,
        String model,
        Usage usage
    ) {}
    
    record EmbeddingData(
        String object,             // "embedding"
        List<Double> embedding,
        int index
    ) {}
    
    record Usage(
        int prompt_tokens,
        int total_tokens
    ) {}
    
    /**
     * Error response de OpenAI API
     */
    record OpenAIError(
        ErrorDetail error
    ) {}
    
    record ErrorDetail(
        String message,
        String type,
        String param,
        String code
    ) {}
    
    @Override
    public String model() {
        return model;
    }
    
    @Override
    public float[] embedOne(String text) {
        log.debug("Generando embedding para texto de {} caracteres con modelo {}", 
                text.length(), model);
        
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("El texto no puede estar vacío");
        }
        
        try {
            EmbeddingsRequest request = EmbeddingsRequest.single(text, model, dimensions);
            
            EmbeddingsResponse response = openAiWebClient
                .post()
                .uri("/v1/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(OpenAIError.class)
                        .flatMap(error -> {
                            log.error("Error de OpenAI API: {} - {}", 
                                    error.error().code(), error.error().message());
                            return Mono.error(new RuntimeException(
                                "OpenAI API error: " + error.error().message()
                            ));
                        })
                )
                .bodyToMono(EmbeddingsResponse.class)
                .retryWhen(createRetrySpec("embedOne"))
                .block();
            
            if (response == null || response.data().isEmpty()) {
                throw new RuntimeException("Respuesta vacía de OpenAI API");
            }
            
            log.debug("Embedding generado exitosamente. Tokens usados: {}", 
                    response.usage().total_tokens());
            
            return convertToFloatArray(response.data().get(0).embedding());
            
        } catch (Exception e) {
            log.error("Error al generar embedding después de {} intentos: {}", 
                    maxRetryAttempts, e.getMessage());
            throw new RuntimeException("Error al generar embedding con OpenAI: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<float[]> embedMany(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("La lista de textos no puede estar vacía");
        }
        
        log.debug("Generando embeddings para {} textos con modelo {}", texts.size(), model);
        
        // Validar que ningún texto esté vacío
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i) == null || texts.get(i).isBlank()) {
                throw new IllegalArgumentException("El texto en la posición " + i + " está vacío");
            }
        }
        
        try {
            EmbeddingsRequest request = EmbeddingsRequest.batch(texts, model, dimensions);
            
            EmbeddingsResponse response = openAiWebClient
                .post()
                .uri("/v1/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(OpenAIError.class)
                        .flatMap(error -> {
                            log.error("Error de OpenAI API: {} - {}", 
                                    error.error().code(), error.error().message());
                            return Mono.error(new RuntimeException(
                                "OpenAI API error: " + error.error().message()
                            ));
                        })
                )
                .bodyToMono(EmbeddingsResponse.class)
                .retryWhen(createRetrySpec("embedMany"))
                .block();
            
            if (response == null || response.data().isEmpty()) {
                throw new RuntimeException("Respuesta vacía de OpenAI API");
            }
            
            if (response.data().size() != texts.size()) {
                throw new RuntimeException(String.format(
                    "Número de embeddings (%d) no coincide con número de textos (%d)",
                    response.data().size(), texts.size()
                ));
            }
            
            log.debug("Embeddings generados exitosamente. Tokens usados: {}", 
                    response.usage().total_tokens());
            
            // Ordenar por índice y convertir a float[]
            return response.data().stream()
                .sorted((a, b) -> Integer.compare(a.index(), b.index()))
                .map(data -> convertToFloatArray(data.embedding()))
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error al generar embeddings en batch después de {} intentos: {}", 
                    maxRetryAttempts, e.getMessage());
            throw new RuntimeException("Error al generar embeddings con OpenAI: " + e.getMessage(), e);
        }
    }
    
    /**
     * Crea una especificación de retry con backoff exponencial
     * 
     * Se reintenta en casos de:
     * - Connection reset
     * - Timeouts de red
     * - Errores 5xx del servidor (temporales)
     * 
     * No se reintenta en casos de:
     * - Errores 4xx (errores del cliente, como API key inválida)
     * - Errores de validación
     */
    private Retry createRetrySpec(String operation) {
        return Retry.backoff(maxRetryAttempts, Duration.ofSeconds(minBackoffSeconds))
            .maxBackoff(Duration.ofSeconds(maxBackoffSeconds))
            .filter(this::isRetryableException)
            .doBeforeRetry(retrySignal -> {
                long attempt = retrySignal.totalRetries() + 1;
                Throwable failure = retrySignal.failure();
                log.warn("Reintentando {} (intento {}/{}): {}", 
                        operation, attempt, maxRetryAttempts, failure.getMessage());
            })
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                log.error("Se agotaron los {} reintentos para {}", 
                        maxRetryAttempts, operation);
                return retrySignal.failure();
            });
    }
    
    /**
     * Determina si una excepción es reintentable
     * 
     * @param throwable La excepción a evaluar
     * @return true si se debe reintentar, false en caso contrario
     */
    private boolean isRetryableException(Throwable throwable) {
        // Connection reset o errores de socket
        if (throwable instanceof WebClientRequestException) {
            WebClientRequestException wcre = (WebClientRequestException) throwable;
            Throwable cause = wcre.getCause();
            
            // Connection reset
            if (cause instanceof SocketException) {
                log.debug("Connection reset detectado, reintentando...");
                return true;
            }
            
            // Otros errores de red
            if (cause != null) {
                String message = cause.getMessage();
                if (message != null && (
                    message.contains("Connection reset") ||
                    message.contains("Connection refused") ||
                    message.contains("Broken pipe") ||
                    message.contains("Connection timed out")
                )) {
                    log.debug("Error de red detectado: {}, reintentando...", message);
                    return true;
                }
            }
        }
        
        // Errores del servidor (5xx) son reintentables
        String message = throwable.getMessage();
        if (message != null && (
            message.contains("500") ||
            message.contains("502") ||
            message.contains("503") ||
            message.contains("504") ||
            message.contains("Server Error")
        )) {
            log.debug("Error del servidor detectado, reintentando...");
            return true;
        }
        
        // Timeout errors
        if (throwable.getClass().getSimpleName().contains("Timeout")) {
            log.debug("Timeout detectado, reintentando...");
            return true;
        }
        
        // No reintentar errores 4xx (cliente) ni otros errores
        log.debug("Error no reintentable: {}", throwable.getClass().getSimpleName());
        return false;
    }
    
    /**
     * Convierte una lista de Double a un array de float
     */
    private float[] convertToFloatArray(List<Double> doubleList) {
        float[] floatArray = new float[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
            floatArray[i] = doubleList.get(i).floatValue();
        }
        return floatArray;
    }
}

