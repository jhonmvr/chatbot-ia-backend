package com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relative.chat.bot.ia.domain.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente para interactuar con Meta WhatsApp Business API para plantillas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetaWhatsAppTemplateApiClient {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    /**
     * Crea una plantilla en Meta WhatsApp Business API
     */
    public Mono<MetaTemplateResponse> createTemplate(String accessToken, String businessAccountId, WhatsAppTemplate template) {
        log.info("Creando plantilla en Meta API: {} para business account: {}", template.name(), businessAccountId);
        
        Map<String, Object> requestBody = buildCreateTemplateRequest(template);
        
        WebClient webClient = webClientBuilder
            .baseUrl("https://graph.facebook.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        
        return webClient.post()
            .uri("/v21.0/{businessAccountId}/message_templates", businessAccountId)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(MetaTemplateResponse.class)
            .timeout(Duration.ofSeconds(30))
            .doOnSuccess(response -> log.info("Plantilla creada exitosamente en Meta: {}", response.id()))
            .doOnError(error -> log.error("Error al crear plantilla en Meta: {}", error.getMessage()));
    }
    
    /**
     * Obtiene el estado de una plantilla desde Meta API
     */
    public Mono<MetaTemplateStatusResponse> getTemplateStatus(String accessToken, String templateId) {
        log.debug("Obteniendo estado de plantilla desde Meta API: {}", templateId);
        
        WebClient webClient = webClientBuilder
            .baseUrl("https://graph.facebook.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();
        
        return webClient.get()
            .uri("/v21.0/{templateId}?fields=status,quality_score", templateId)
            .retrieve()
            .bodyToMono(MetaTemplateStatusResponse.class)
            .timeout(Duration.ofSeconds(15))
            .doOnError(error -> log.error("Error al obtener estado de plantilla {}: {}", templateId, error.getMessage()));
    }
    
    /**
     * Lista todas las plantillas de una cuenta de negocio
     */
    public Mono<MetaTemplateListResponse> listTemplates(String accessToken, String businessAccountId) {
        log.debug("Listando plantillas desde Meta API para business account: {}", businessAccountId);
        
        WebClient webClient = webClientBuilder
            .baseUrl("https://graph.facebook.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();
        
        return webClient.get()
            .uri("/v21.0/{businessAccountId}/message_templates", businessAccountId)
            .retrieve()
            .bodyToMono(MetaTemplateListResponse.class)
            .timeout(Duration.ofSeconds(15))
            .doOnError(error -> log.error("Error al listar plantillas: {}", error.getMessage()));
    }
    
    /**
     * Elimina una plantilla de Meta API
     */
    public Mono<Void> deleteTemplate(String accessToken, String templateId) {
        log.info("Eliminando plantilla de Meta API: {}", templateId);
        
        WebClient webClient = webClientBuilder
            .baseUrl("https://graph.facebook.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();
        
        return webClient.delete()
            .uri("/v21.0/{templateId}", templateId)
            .retrieve()
            .bodyToMono(Void.class)
            .timeout(Duration.ofSeconds(15))
            .doOnSuccess(response -> log.info("Plantilla eliminada exitosamente de Meta: {}", templateId))
            .doOnError(error -> log.error("Error al eliminar plantilla {}: {}", templateId, error.getMessage()));
    }
    
    /**
     * Construye el request body para crear una plantilla
     */
    private Map<String, Object> buildCreateTemplateRequest(WhatsAppTemplate template) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", template.name());
        request.put("category", template.category().name().toLowerCase());
        request.put("language", template.language());
        
        if (template.parameterFormat() != null) {
            request.put("parameter_format", template.parameterFormat().name().toLowerCase());
        }
        
        // Construir componentes
        List<Map<String, Object>> components = template.components().stream()
            .map(this::buildComponentRequest)
            .toList();
        
        request.put("components", components);
        
        return request;
    }
    
    /**
     * Construye el request para un componente
     */
    private Map<String, Object> buildComponentRequest(TemplateComponent component) {
        Map<String, Object> componentRequest = new HashMap<>();
        componentRequest.put("type", component.type().name().toLowerCase());
        
        if (component.text() != null) {
            componentRequest.put("text", component.text());
        }
        
        // Construir parámetros
        if (component.parameters() != null && !component.parameters().isEmpty()) {
            List<Map<String, Object>> parameters = component.parameters().stream()
                .map(this::buildParameterRequest)
                .toList();
            componentRequest.put("parameters", parameters);
        }
        
        // Construir botones
        if (component.buttons() != null && !component.buttons().isEmpty()) {
            List<Map<String, Object>> buttons = component.buttons().stream()
                .map(this::buildButtonRequest)
                .toList();
            componentRequest.put("buttons", buttons);
        }
        
        // Construir media
        if (component.media() != null) {
            Map<String, Object> media = buildMediaRequest(component.media());
            componentRequest.put("media", media);
        }
        
        return componentRequest;
    }
    
    /**
     * Construye el request para un parámetro
     */
    private Map<String, Object> buildParameterRequest(ComponentParameter parameter) {
        Map<String, Object> paramRequest = new HashMap<>();
        paramRequest.put("type", parameter.type());
        paramRequest.put("text", parameter.text());
        
        if (parameter.parameterName() != null) {
            paramRequest.put("parameter_name", parameter.parameterName());
        }
        
        if (parameter.example() != null) {
            paramRequest.put("example", parameter.example());
        }
        
        return paramRequest;
    }
    
    /**
     * Construye el request para un botón
     */
    private Map<String, Object> buildButtonRequest(TemplateButton button) {
        Map<String, Object> buttonRequest = new HashMap<>();
        buttonRequest.put("type", button.type());
        buttonRequest.put("text", button.text());
        
        if (button.url() != null) {
            buttonRequest.put("url", button.url());
        }
        
        if (button.phoneNumber() != null) {
            buttonRequest.put("phone_number", button.phoneNumber());
        }
        
        return buttonRequest;
    }
    
    /**
     * Construye el request para media
     */
    private Map<String, Object> buildMediaRequest(MediaComponent media) {
        Map<String, Object> mediaRequest = new HashMap<>();
        mediaRequest.put("type", media.type());
        
        if (media.url() != null) {
            mediaRequest.put("url", media.url());
        }
        
        if (media.mediaId() != null) {
            mediaRequest.put("media_id", media.mediaId());
        }
        
        if (media.filename() != null) {
            mediaRequest.put("filename", media.filename());
        }
        
        if (media.altText() != null) {
            mediaRequest.put("alt_text", media.altText());
        }
        
        return mediaRequest;
    }
    
    /**
     * Maneja errores de la API de Meta
     */
    private Mono<MetaTemplateResponse> handleError(Throwable error) {
        if (error instanceof WebClientResponseException webClientError) {
            log.error("Error de Meta API - Status: {}, Body: {}", 
                webClientError.getStatusCode(), webClientError.getResponseBodyAsString());
            
            // Intentar parsear el error de Meta
            try {
                String errorBody = webClientError.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                
                return Mono.error(new MetaApiException(
                    "Error de Meta API: " + errorMap.get("error"),
                    webClientError.getStatusCode().value()
                ));
            } catch (Exception parseError) {
                return Mono.error(new MetaApiException(
                    "Error de Meta API: " + webClientError.getResponseBodyAsString(),
                    webClientError.getStatusCode().value()
                ));
            }
        }
        
        return Mono.error(new MetaApiException("Error desconocido: " + error.getMessage(), 500));
    }
    
    /**
     * Excepción personalizada para errores de Meta API
     */
    public static class MetaApiException extends RuntimeException {
        private final int statusCode;
        
        public MetaApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
    }
    
    /**
     * Respuesta de creación de plantilla
     */
    public record MetaTemplateResponse(String id, String status) {}
    
    /**
     * Respuesta de estado de plantilla
     */
    public record MetaTemplateStatusResponse(String id, String status, String quality_score) {}
    
    /**
     * Respuesta de lista de plantillas
     */
    public record MetaTemplateListResponse(List<MetaTemplateItem> data) {}
    
    /**
     * Item de plantilla en la lista
     */
    public record MetaTemplateItem(String id, String name, String status, String category) {}
}
