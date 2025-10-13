package com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp;

import com.relative.chat.bot.ia.application.ports.out.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación de WhatsAppService usando Meta WhatsApp Business API
 * 
 * Documentación oficial: https://developers.facebook.com/docs/whatsapp/cloud-api
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.whatsapp.provider", havingValue = "meta", matchIfMissing = false)
public class MetaWhatsAppAdapter implements WhatsAppService {
    
    private final WebClient metaClient;
    private final String phoneNumberId;
    
    public MetaWhatsAppAdapter(
            @Value("${app.whatsapp.meta.access-token:#{null}}") String accessToken,
            @Value("${app.whatsapp.meta.phone-number-id:#{null}}") String phoneNumberId,
            @Value("${app.whatsapp.meta.api-version:v21.0}") String apiVersion
    ) {
        this.phoneNumberId = phoneNumberId;
        
        if (accessToken != null && phoneNumberId != null) {
            this.metaClient = WebClient.builder()
                    .baseUrl("https://graph.facebook.com/" + apiVersion)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            
            log.info("MetaWhatsAppAdapter inicializado con Phone Number ID: {}", phoneNumberId);
        } else {
            log.warn("Credenciales de Meta WhatsApp no configuradas. El servicio no funcionará.");
            this.metaClient = null;
        }
    }
    
    @Override
    public String sendMessage(String from, String to, String message) {
        if (metaClient == null) {
            log.error("No se puede enviar mensaje: Meta WhatsApp no está configurado");
            throw new IllegalStateException("Meta WhatsApp no está configurado");
        }
        
        try {
            // Limpiar número de teléfono (solo dígitos)
            String cleanTo = cleanPhoneNumber(to);
            
            log.info("Enviando mensaje a {}: {}", cleanTo, 
                    message.substring(0, Math.min(50, message.length())));
            
            // Construir payload según API de Meta
            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("recipient_type", "individual");
            payload.put("to", cleanTo);
            
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("preview_url", false);
            textContent.put("body", message);
            
            payload.put("type", "text");
            payload.put("text", textContent);
            
            // Enviar solicitud
            @SuppressWarnings("unchecked")
            Map<String, Object> response = metaClient.post()
                    .uri("/{phone_number_id}/messages", phoneNumberId)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && response.containsKey("messages")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> messages = (List<Map<String, String>>) response.get("messages");
                if (!messages.isEmpty()) {
                    String messageId = messages.get(0).get("id");
                    log.info("Mensaje enviado exitosamente. ID: {}", messageId);
                    return messageId;
                }
            }
            
            log.error("Respuesta inesperada de Meta API: {}", response);
            throw new RuntimeException("No se pudo obtener el ID del mensaje");
            
        } catch (Exception e) {
            log.error("Error al enviar mensaje de WhatsApp: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar mensaje: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String sendTemplate(String from, String to, String templateId, Map<String, String> parameters) {
        if (metaClient == null) {
            log.error("No se puede enviar plantilla: Meta WhatsApp no está configurado");
            throw new IllegalStateException("Meta WhatsApp no está configurado");
        }
        
        try {
            String cleanTo = cleanPhoneNumber(to);
            
            log.info("Enviando plantilla '{}' a {}", templateId, cleanTo);
            
            // Construir payload para template
            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", cleanTo);
            payload.put("type", "template");
            
            // Template structure
            Map<String, Object> template = new HashMap<>();
            template.put("name", templateId);
            template.put("language", Map.of("code", "es"));  // Español por defecto
            
            // Agregar componentes si hay parámetros
            if (parameters != null && !parameters.isEmpty()) {
                List<Map<String, Object>> components = buildTemplateComponents(parameters);
                template.put("components", components);
            }
            
            payload.put("template", template);
            
            // Enviar solicitud
            @SuppressWarnings("unchecked")
            Map<String, Object> response = metaClient.post()
                    .uri("/{phone_number_id}/messages", phoneNumberId)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && response.containsKey("messages")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> messages = (List<Map<String, String>>) response.get("messages");
                if (!messages.isEmpty()) {
                    String messageId = messages.get(0).get("id");
                    log.info("Plantilla enviada exitosamente. ID: {}", messageId);
                    return messageId;
                }
            }
            
            log.error("Respuesta inesperada de Meta API: {}", response);
            throw new RuntimeException("No se pudo obtener el ID del mensaje");
            
        } catch (Exception e) {
            log.error("Error al enviar plantilla de WhatsApp: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar plantilla: " + e.getMessage(), e);
        }
    }
    
    /**
     * Limpia el número de teléfono para que solo contenga dígitos
     * Ejemplo: +593 99 999 9999 -> 593999999999
     */
    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Número de teléfono no puede estar vacío");
        }
        
        // Remover todos los caracteres que no sean dígitos
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Número de teléfono inválido: " + phoneNumber);
        }
        
        return cleaned;
    }
    
    /**
     * Construye los componentes de la plantilla con los parámetros
     */
    private List<Map<String, Object>> buildTemplateComponents(Map<String, String> parameters) {
        // Construir parámetros del body
        List<Map<String, Object>> bodyParameters = parameters.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "type", "text",
                        "text", entry.getValue()
                ))
                .toList();
        
        Map<String, Object> bodyComponent = new HashMap<>();
        bodyComponent.put("type", "body");
        bodyComponent.put("parameters", bodyParameters);
        
        return List.of(bodyComponent);
    }
}

