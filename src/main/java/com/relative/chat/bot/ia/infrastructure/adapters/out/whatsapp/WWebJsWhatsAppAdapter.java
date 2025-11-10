package com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp;

import com.relative.chat.bot.ia.application.ports.out.WhatsAppService;
import com.relative.chat.bot.ia.application.services.WhatsAppProviderConfigServiceV2;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.messaging.ComponentType;
import com.relative.chat.bot.ia.domain.messaging.TemplateComponent;
import com.relative.chat.bot.ia.domain.messaging.WhatsAppTemplate;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.WhatsAppTemplateRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.dto.SendResponse;
import com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.dto.SendTemplateRequest;
import com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.dto.SendTextRequest;
import com.relative.chat.bot.ia.infrastructure.config.WWebJsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class WWebJsWhatsAppAdapter implements WhatsAppService {

    private final WhatsAppProviderConfigServiceV2 configServiceV2;
    private final ClientPhoneRepository clientPhoneRepository;
    private final WhatsAppTemplateRepository whatsAppTemplateRepository;

    @Override
    public String sendMessage(String from, String to, String message) {
        try {
            // Obtener configuración usando la nueva arquitectura parametrizable
            Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> configOpt =
                    getWWebJsConfiguration(from);

            if (configOpt.isEmpty()) {
                log.error("No se encontró configuración de WWebJs para el número: {}", from);
                throw new IllegalStateException("Configuración de WWebJs no encontrada para: " + from);
            }

            WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configOpt.get();

            // Crear cliente WebClient dinámicamente
            WebClient wwebjsClient = createWebClient(config);

            // Limpiar número de teléfono (solo dígitos)
            String cleanTo = cleanPhoneNumber(to);

            log.info("Enviando mensaje a {} desde {}: {}", cleanTo, from,
                    message.substring(0, Math.min(50, message.length())));

            // Construir payload según API de WWebJs
            Map<String, Object> payload = new HashMap<>();
            payload.put("sessionId", from);
            payload.put("to", cleanTo);
            payload.put("message", message);

            // Enviar solicitud
            @SuppressWarnings("unchecked")
            Map<String, Object> response = wwebjsClient.post()
                    .uri(config.getConfigValueOrDefault("send_text_path", "/send-text"))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                String messageId = (String) response.get("id");
                log.info("Mensaje enviado exitosamente. ID: {}", messageId);
                return messageId;
            }

            log.error("Respuesta inesperada de WWebJs API: {}", response);
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
                    getWWebJsConfiguration(from);

            if (configOpt.isEmpty()) {
                log.error("No se encontró configuración de WWebJs para el número: {}", from);
                throw new IllegalStateException("Configuración de WWebJs no encontrada para: " + from);
            }

            WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configOpt.get();

            log.info("config {}", configOpt);

            // Crear cliente WebClient dinámicamente
            WebClient wwebjsClient = createWebClient(config);

            String cleanTo = cleanPhoneNumber(to);

            log.info("Enviando plantilla '{}' a {} desde {}", templateId, cleanTo, from);

            // Construir el mensaje interpolando los parámetros en el template
            String interpolatedMessage = buildMessageFromTemplate(templateId, parameters);

            // Construir payload como mensaje de texto simple
            Map<String, Object> payload = new HashMap<>();
            payload.put("sessionId", config.getConfigValue("sessionId"));
            payload.put("to", cleanTo);
            payload.put("message", interpolatedMessage);

            // Enviar solicitud usando el mismo endpoint que sendMessage
            @SuppressWarnings("unchecked")
            Map<String, Object> response = wwebjsClient.post()
                    .uri(config.getConfigValueOrDefault("send_text_path", "/send-text"))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                String messageId = (String) response.get("id");
                log.info("Plantilla enviada exitosamente. ID: {}", messageId);
                return messageId;
            }

            log.error("Respuesta inesperada de WWebJs API: {}", response);
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
     * Construye el mensaje interpolando los parámetros en el template
     * Los parámetros se reemplazan en orden: {{1}}, {{2}}, {{3}}, etc.
     */
    private String buildMessageFromTemplate(String templateId, Map<String, String> parameters) {
        // El templateId contiene el mensaje base con placeholders
        // todo hacer busqueda con id no con nombre
        WhatsAppTemplate template = whatsAppTemplateRepository.findByName(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + templateId));

            String message = template.components().stream().filter( comp -> comp.type().equals(ComponentType.BODY)).findFirst().map(TemplateComponent::text).orElse("");

            if (parameters != null && !parameters.isEmpty()) {
                // Ordenar las claves para reemplazar en orden: 1, 2, 3, etc.
                List<String> sortedKeys = parameters.keySet().stream()
                        .sorted()
                        .toList();

                for (String key : sortedKeys) {
                    String placeholder = "{{" + key + "}}";
                    String value = parameters.get(key);
                    message = message.replace(placeholder, value != null ? value : "");
                }
            }

        return message;
    }

    /**
     * Obtiene la configuración de WWebJs usando la nueva arquitectura parametrizable
     *
     * @param fromNumber Número de origen (phone_number_id de WWebJs o número E164)
     * @return Configuración de WWebJs
     */
    private Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> getWWebJsConfiguration(String fromNumber) {
        // El parámetro 'from' puede ser el phone_number_id de WWebJs o el número E164
        // Primero intentamos buscar por provider_sid (phone_number_id)
        log.info("numero de envio: {}", fromNumber);
        Optional<ClientPhone> phoneByProviderSid = clientPhoneRepository.findByProviderSid(fromNumber, "WWEBJS");

        if (phoneByProviderSid.isPresent()) {
            return configServiceV2.getProviderConfiguration(phoneByProviderSid.get().id(), "WWEBJS");
        }

        // Si no se encuentra por provider_sid, buscar por número E164
        Optional<ClientPhone> phoneByE164 = clientPhoneRepository.findByPhoneAndChannel(
                fromNumber,
                com.relative.chat.bot.ia.domain.types.Channel.WEB
        ).filter(phone -> "WWEBJS".equalsIgnoreCase(phone.provider()));

        if (phoneByE164.isPresent()) {
            return configServiceV2.getProviderConfiguration(phoneByE164.get().id(), "WWEBJS");
        }

        log.warn("No se encontró configuración de WWebJs para: {}", fromNumber);
        return Optional.empty();
    }

    /**
     * Crea un cliente WebClient para WWebJs con la configuración proporcionada
     *
     * @param config Configuración de WWebJs
     * @return Cliente WebClient configurado
     */
    private WebClient createWebClient(WhatsAppProviderConfigServiceV2.ProviderConfiguration config) {
        String fullApiUrl = config.getFullApiUrl();
        if (fullApiUrl.isEmpty()) {
            throw new IllegalStateException("URL de API de WWebJs no está configurada");
        }

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(fullApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        // Si existe un token de autorización, agregarlo
        /*
        String authToken = config.getConfigValueOrDefault("auth_token", "");
        if (!authToken.isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
        }
         */

        return builder.build();
    }
}