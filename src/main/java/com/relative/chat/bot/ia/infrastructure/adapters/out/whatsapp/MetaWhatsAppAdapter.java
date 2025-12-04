package com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp;

import com.relative.chat.bot.ia.application.ports.out.WhatsAppService;
import com.relative.chat.bot.ia.application.services.WhatsAppProviderConfigServiceV2;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación de WhatsAppService usando Meta WhatsApp Business API
 * 
 * Documentación oficial: https://developers.facebook.com/docs/whatsapp/cloud-api
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetaWhatsAppAdapter implements WhatsAppService {
    
    private final WhatsAppProviderConfigServiceV2 configServiceV2;
    private final ClientPhoneRepository clientPhoneRepository;
    
    @Override
    public String sendMessage(String from, String to, String message) {
        try {
            // Obtener configuración usando la nueva arquitectura parametrizable
            Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> configOpt = 
                    getMetaConfiguration(from);
            
            if (configOpt.isEmpty()) {
                log.error("No se encontró configuración de Meta WhatsApp para el número: {}", from);
                throw new IllegalStateException("Configuración de Meta WhatsApp no encontrada para: " + from);
            }
            
            WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configOpt.get();
            
            // Crear cliente WebClient dinámicamente
            WebClient metaClient = createWebClient(config);
            
            // Limpiar número de teléfono (solo dígitos)
            String cleanTo = cleanPhoneNumber(to);
            
            log.info("Enviando mensaje a {} desde {}: {}", cleanTo, from, 
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
                    .uri("/{phone_number_id}/messages", config.getConfigValueOrDefault("phone_number_id", ""))
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
    public String sendTemplate(String from, String to, String templateId, String language, Map<String, String> parameters) {
        try {
            // Obtener configuración usando la nueva arquitectura parametrizable
            Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> configOpt = 
                    getMetaConfiguration(from);
            
            if (configOpt.isEmpty()) {
                log.error("No se encontró configuración de Meta WhatsApp para el número: {}", from);
                throw new IllegalStateException("Configuración de Meta WhatsApp no encontrada para: " + from);
            }
            
            WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configOpt.get();
            
            // Crear cliente WebClient dinámicamente
            WebClient metaClient = createWebClient(config);
            
            String cleanTo = cleanPhoneNumber(to);
            
            log.info("Enviando plantilla '{}' a {} desde {}", templateId, cleanTo, from);
            
            // Construir payload para template
            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", cleanTo);
            payload.put("type", "template");
            
            // Template structure
            Map<String, Object> template = new HashMap<>();
            template.put("name", templateId);
            template.put("language", Map.of("code", language != null && !language.isEmpty() ? language : "es_ES"));
            
            // Agregar componentes si hay parámetros
            if (parameters != null && !parameters.isEmpty()) {
                List<Map<String, Object>> components = buildTemplateComponents(parameters);
                template.put("components", components);
            }
            
            payload.put("template", template);
            
            // Enviar solicitud
            @SuppressWarnings("unchecked")
            Map<String, Object> response = metaClient.post()
                    .uri("/{phone_number_id}/messages", config.getConfigValueOrDefault("phone_number_id", ""))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && response.containsKey("messages")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> messages = (List<Map<String, String>>) response.get("messages");
                if (!messages.isEmpty()) {
                    String messageId = messages.get(0).get("id");
                    String messageStatus = messages.get(0).get("message_status");
                    if(!messageStatus.equals("accepted"))
                        throw new RuntimeException("No se pudo enviar el mensaje");
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
    
    /**
     * Obtiene la configuración de Meta WhatsApp usando la nueva arquitectura parametrizable
     * 
     * @param fromNumber Número de origen (phone_number_id de Meta o número E164)
     * @return Configuración de Meta WhatsApp
     */
    private Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> getMetaConfiguration(String fromNumber) {
        // El parámetro 'from' puede ser el phone_number_id de Meta o el número E164
        // Primero intentamos buscar por provider_sid (phone_number_id)
        Optional<ClientPhone> phoneByProviderSid = clientPhoneRepository.findByProviderSid(fromNumber, "META");
        
        if (phoneByProviderSid.isPresent()) {
            return configServiceV2.getProviderConfiguration(phoneByProviderSid.get().id(), "META");
        }
        
        // Si no se encuentra por provider_sid, buscar por número E164
        Optional<ClientPhone> phoneByE164 = clientPhoneRepository.findByPhoneAndChannel(
                fromNumber, 
                com.relative.chat.bot.ia.domain.types.Channel.WHATSAPP
        ).filter(phone -> "META".equalsIgnoreCase(phone.provider()));
        
        if (phoneByE164.isPresent()) {
            return configServiceV2.getProviderConfiguration(phoneByE164.get().id(), "META");
        }
        
        log.warn("No se encontró configuración de Meta WhatsApp para: {}", fromNumber);
        return Optional.empty();
    }
    
    /**
     * Crea un cliente WebClient para Meta WhatsApp con la configuración proporcionada
     * 
     * @param config Configuración de Meta WhatsApp
     * @return Cliente WebClient configurado
     */
    private WebClient createWebClient(WhatsAppProviderConfigServiceV2.ProviderConfiguration config) {
        String accessToken = config.getConfigValueOrDefault("access_token", "");
        if (accessToken.isEmpty()) {
            throw new IllegalStateException("Access token de Meta WhatsApp no está configurado");
        }
        String apiVersion = config.getConfigValueOrDefault("api_version", "");
        String fullApiUrl;
        if (apiVersion.isEmpty()) {
            fullApiUrl = config.getFullApiUrl() ;
        }else{
            fullApiUrl = config.getApiBaseUrl() + "/" + apiVersion;
        }
        if (fullApiUrl.isEmpty()) {
            throw new IllegalStateException("URL de API de Meta WhatsApp no está configurada");
        }
        return WebClient.builder()
                .baseUrl(fullApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}

