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
    public Mono<MetaTemplateResponse> createTemplate(String accessToken, String businessAccountId, String apiVersion, WhatsAppTemplate template) {
        log.info("Creando plantilla en Meta API: {} para business account: {} usando versión: {}", 
                template.name(), businessAccountId, apiVersion);
        
        Map<String, Object> requestBody = buildCreateTemplateRequest(template);
        
        WebClient webClient = webClientBuilder
            .baseUrl("https://graph.facebook.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        
        return webClient.post()
            .uri("/{apiVersion}/{businessAccountId}/message_templates", apiVersion, businessAccountId)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(MetaTemplateResponse.class)
            .timeout(Duration.ofSeconds(30))
            .doOnSuccess(response -> log.info("Plantilla creada exitosamente en Meta: {}", response.id()))
            .onErrorMap(WebClientResponseException.class, this::parseMetaError);
    }
    
    /**
     * Obtiene el estado de una plantilla desde Meta API
     */
    public Mono<MetaTemplateStatusResponse> getTemplateStatus(String accessToken, String apiVersion, String templateId) {
        log.debug("Obteniendo estado de plantilla desde Meta API: {} usando versión: {}", templateId, apiVersion);
        
        WebClient webClient = webClientBuilder
            .baseUrl("https://graph.facebook.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();
        
        return webClient.get()
            .uri("/{apiVersion}/{templateId}?fields=status,quality_score", apiVersion, templateId)
            .retrieve()
            .bodyToMono(MetaTemplateStatusResponse.class)
            .timeout(Duration.ofSeconds(15))
            .onErrorMap(WebClientResponseException.class, this::parseMetaError);
    }
    
    /**
     * Lista todas las plantillas de una cuenta de negocio
     */
    public Mono<MetaTemplateListResponse> listTemplates(String accessToken, String apiVersion, String businessAccountId) {
        log.debug("Listando plantillas desde Meta API para business account: {} usando versión: {}", 
                businessAccountId, apiVersion);
        
        WebClient webClient = webClientBuilder
            .baseUrl("https://graph.facebook.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();
        
        return webClient.get()
            .uri("/{apiVersion}/{businessAccountId}/message_templates", apiVersion, businessAccountId)
            .retrieve()
            .bodyToMono(MetaTemplateListResponse.class)
            .timeout(Duration.ofSeconds(15))
            .onErrorMap(WebClientResponseException.class, this::parseMetaError);
    }
    
    /**
     * Elimina una plantilla de Meta API
     */
    public Mono<Void> deleteTemplate(String accessToken, String apiVersion, String templateId) {
        log.info("Eliminando plantilla de Meta API: {} usando versión: {}", templateId, apiVersion);
        
        WebClient webClient = webClientBuilder
            .baseUrl("https://graph.facebook.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();
        
        return webClient.delete()
            .uri("/{apiVersion}/{templateId}", apiVersion, templateId)
            .retrieve()
            .bodyToMono(Void.class)
            .timeout(Duration.ofSeconds(15))
            .doOnSuccess(response -> log.info("Plantilla eliminada exitosamente de Meta: {}", templateId))
            .onErrorMap(WebClientResponseException.class, this::parseMetaError);
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
        
        // Campo format para HEADER (TEXT, IMAGE, DOCUMENT, VIDEO, LOCATION)
        if (component.format() != null) {
            componentRequest.put("format", component.format());
        }
        
        // Texto del componente
        if (component.text() != null) {
            componentRequest.put("text", component.text());
            
            // Si el texto tiene placeholders ({{variable}}), agregar ejemplo
            if (hasPlaceholders(component.text()) && component.parameters() != null) {
                Map<String, Object> example = buildExampleForComponent(component);
                if (!example.isEmpty()) {
                    componentRequest.put("example", example);
                }
            }
        }
        
        // Campos especiales para AUTHENTICATION templates
        if (component.addSecurityRecommendation() != null) {
            componentRequest.put("add_security_recommendation", component.addSecurityRecommendation());
        }
        
        if (component.codeExpirationMinutes() != null) {
            componentRequest.put("code_expiration_minutes", component.codeExpirationMinutes());
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
     * Verifica si un texto contiene placeholders
     */
    private boolean hasPlaceholders(String text) {
        return text != null && text.contains("{{");
    }
    
    /**
     * Construye el objeto example para un componente basado en sus parámetros
     */
    private Map<String, Object> buildExampleForComponent(TemplateComponent component) {
        Map<String, Object> example = new HashMap<>();
        
        if (component.parameters() == null || component.parameters().isEmpty()) {
            return example;
        }
        
        // Extraer los ejemplos de los parámetros en el orden que aparecen
        List<String> examples = component.parameters().stream()
            .filter(param -> param.example() != null && !param.example().isEmpty())
            .map(ComponentParameter::example)
            .toList();
        
        if (examples.isEmpty()) {
            log.warn("El componente tiene placeholders pero no tiene ejemplos definidos");
            return example;
        }
        
        // Construir el objeto example según el tipo de componente
        String componentType = component.type().name().toLowerCase();
        
        switch (componentType) {
            case "body":
                example.put("body_text", List.of(examples));
                break;
            case "header":
                // Para HEADER, puede ser header_text o header_handle
                example.put("header_text", List.of(examples));
                break;
            case "footer":
                example.put("footer_text", List.of(examples));
                break;
            default:
                log.debug("Tipo de componente {} no requiere ejemplo", componentType);
        }
        
        return example;
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
        
        // Campos para botones OTP
        if (button.otpType() != null) {
            buttonRequest.put("otp_type", button.otpType());
        }
        
        if (button.autofillText() != null) {
            buttonRequest.put("autofill_text", button.autofillText());
        }
        
        if (button.packageName() != null) {
            buttonRequest.put("package_name", button.packageName());
        }
        
        if (button.signatureHash() != null) {
            buttonRequest.put("signature_hash", button.signatureHash());
        }
        
        if (button.example() != null) {
            buttonRequest.put("example", List.of(button.example()));
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
     * Parsea el error de Meta API y extrae información detallada
     */
    private MetaApiException parseMetaError(WebClientResponseException webClientError) {
        log.error("Error de Meta API - Status: {}, Body: {}", 
            webClientError.getStatusCode(), webClientError.getResponseBodyAsString());
        
        try {
            String errorBody = webClientError.getResponseBodyAsString();
            @SuppressWarnings("unchecked")
            Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
            
            // Extraer objeto error
            @SuppressWarnings("unchecked")
            Map<String, Object> errorObj = (Map<String, Object>) errorMap.get("error");
            
            if (errorObj != null) {
                String message = (String) errorObj.get("message");
                String errorUserMsg = (String) errorObj.get("error_user_msg");
                String errorUserTitle = (String) errorObj.get("error_user_title");
                String errorType = (String) errorObj.get("type");
                Integer errorCode = (Integer) errorObj.get("code");
                
                // Usar error_user_msg como mensaje principal si está disponible
                String finalMessage = errorUserMsg != null ? errorUserMsg : message;
                
                return new MetaApiException(
                    finalMessage,
                    webClientError.getStatusCode().value(),
                    errorUserMsg,
                    errorUserTitle,
                    errorType,
                    errorCode
                );
            } else {
                return new MetaApiException(
                    "Error de Meta API: " + errorMap.get("error"),
                    webClientError.getStatusCode().value()
                );
            }
        } catch (Exception parseError) {
            log.warn("No se pudo parsear el error de Meta API: {}", parseError.getMessage());
            return new MetaApiException(
                "Error de Meta API: " + webClientError.getResponseBodyAsString(),
                webClientError.getStatusCode().value()
            );
        }
    }
    
    /**
     * Excepción personalizada para errores de Meta API
     */
    public static class MetaApiException extends RuntimeException {
        private final int statusCode;
        private final String errorUserMsg;
        private final String errorUserTitle;
        private final String errorType;
        private final Integer errorCode;
        
        public MetaApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
            this.errorUserMsg = null;
            this.errorUserTitle = null;
            this.errorType = null;
            this.errorCode = null;
        }
        
        public MetaApiException(String message, int statusCode, String errorUserMsg, 
                               String errorUserTitle, String errorType, Integer errorCode) {
            super(message);
            this.statusCode = statusCode;
            this.errorUserMsg = errorUserMsg;
            this.errorUserTitle = errorUserTitle;
            this.errorType = errorType;
            this.errorCode = errorCode;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public String getErrorUserMsg() {
            return errorUserMsg != null ? errorUserMsg : getMessage();
        }
        
        public String getErrorUserTitle() {
            return errorUserTitle;
        }
        
        public String getErrorType() {
            return errorType;
        }
        
        public Integer getErrorCode() {
            return errorCode;
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
