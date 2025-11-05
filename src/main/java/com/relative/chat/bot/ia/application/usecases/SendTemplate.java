package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.services.WhatsAppProviderConfigServiceV2;
import com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.WhatsAppProviderRouter;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.*;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.MessageRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.WhatsAppTemplateRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.types.Direction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Caso de uso: Enviar mensaje con plantilla
 * 
 * Basado en la documentación de WhatsApp Business Management API:
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/message-templates
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/authentication-templates
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/message-templates/marketing-templates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendTemplate {
    
    private final MessageRepository messageRepository;
    private final WhatsAppProviderRouter whatsAppRouter;
    private final ClientPhoneRepository clientPhoneRepository;
    private final WhatsAppTemplateRepository templateRepository;
    private final ConversationRepository conversationRepository;
    private final WhatsAppProviderConfigServiceV2 configServiceV2;
    
    /**
     * Envía un mensaje con plantilla
     * 
     * @param clientId ID del cliente
     * @param conversationId ID de la conversación
     * @param contactId ID del contacto (opcional)
     * @param phoneId ID del teléfono del cliente
     * @param templateName Nombre de la plantilla
     * @param parameters Parámetros de la plantilla
     * @param parameterFormat Formato de parámetros (NAMED o POSITIONAL)
     * @param toNumber Número de destino
     * @return Mensaje enviado
     */
    @Transactional
    public Message handle(
            UuidId<Client> clientId,
            UuidId<Conversation> conversationId,
            UuidId<Contact> contactId,
            UuidId<ClientPhone> phoneId,
            String templateName,
            Map<String, String> parameters,
            ParameterFormat parameterFormat,
            String toNumber
    ) {
        // 1. Validar que el teléfono existe
        Optional<ClientPhone> phoneOpt = clientPhoneRepository.findById(phoneId);
        if (phoneOpt.isEmpty()) {
            throw new IllegalArgumentException("Teléfono no encontrado: " + phoneId.value());
        }
        
        ClientPhone phone = phoneOpt.get();
        
        // 2. Validar que la plantilla existe y está aprobada
        Optional<WhatsAppTemplate> templateOpt = templateRepository
                .findByClientPhoneIdAndNameAndCategory(phoneId, templateName, null);
        
        if (templateOpt.isEmpty()) {
            throw new IllegalStateException("Plantilla '" + templateName + "' no encontrada para el teléfono: " + phoneId.value());
        }
        
        WhatsAppTemplate template = templateOpt.get();
        
        // 3. Validar estado de la plantilla
        if (template.status() != TemplateStatus.APPROVED) {
            throw new IllegalStateException(
                String.format("Plantilla '%s' no está aprobada. Estado actual: %s", 
                    templateName, template.status())
            );
        }
        
        // 4. Validar calidad de la plantilla
        if (template.qualityRating() == QualityRating.LOW) {
            log.warn("Plantilla '{}' tiene calidad baja. Riesgo de pausa.", templateName);
        }
        
        // 5. Validar parámetros según el formato
        validateParameters(template, parameters, parameterFormat);
        
        // 6. Obtener o crear conversación
        UuidId<Conversation> finalConversationId = getOrCreateConversation(clientId, contactId, phoneId, conversationId);
        
        // 7. Crear el mensaje
        Message message = new Message(
                UuidId.newId(),
                clientId,
                finalConversationId,
                contactId,
                phoneId,
                Channel.WHATSAPP,
                Direction.OUT,
                buildTemplateContent(templateName, parameters, parameterFormat),
                Instant.now()
        );
        
        try {
            // 8. Enviar a través del servicio externo
            if (whatsAppRouter != null) {
                // Determinar el número de origen y el provider basándose en la configuración del cliente
                ProviderInfo providerInfo = determineProviderInfo(phoneId, phone);
                
                // Obtener el idioma de la plantilla
                String language = extractLanguageCode(template.language());
                
                String externalId = whatsAppRouter.sendTemplate(
                    providerInfo.fromNumber(), 
                    toNumber, 
                    templateName, 
                    language,
                    parameters,
                    providerInfo.provider()
                );
                message.markSent(Instant.now(), externalId);
                log.info("Plantilla '{}' enviada exitosamente usando provider {}. ID externo: {}", 
                    templateName, providerInfo.provider(), externalId);
            }
        } catch (Exception e) {
            log.error("Error al enviar plantilla '{}': {}", templateName, e.getMessage(), e);
            message.fail(e.getMessage());
        }
        
        // 9. Guardar el mensaje
        messageRepository.save(message);
        
        return message;
    }
    
    /**
     * Valida los parámetros según el formato especificado
     */
    private void validateParameters(WhatsAppTemplate template, Map<String, String> parameters, ParameterFormat format) {
        List<TemplateComponent> components = template.components();
        
        for (TemplateComponent component : components) {
            if (component.type() == ComponentType.BODY && component.parameters() != null) {
                List<ComponentParameter> componentParams = component.parameters();
                
                if (format == ParameterFormat.NAMED) {
                    validateNamedParameters(componentParams, parameters);
                } else if (format == ParameterFormat.POSITIONAL) {
                    validatePositionalParameters(componentParams, parameters);
                }
            }
        }
    }
    
    /**
     * Valida parámetros con formato NAMED
     */
    private void validateNamedParameters(List<ComponentParameter> componentParams, Map<String, String> parameters) {
        for (ComponentParameter param : componentParams) {
            if (param.parameterName() != null && !parameters.containsKey(param.parameterName())) {
                throw new IllegalArgumentException(
                    String.format("Parámetro requerido '%s' no encontrado en los parámetros proporcionados", param.parameterName())
                );
            }
        }
    }
    
    /**
     * Valida parámetros con formato POSITIONAL
     */
    private void validatePositionalParameters(List<ComponentParameter> componentParams, Map<String, String> parameters) {
        int expectedCount = componentParams.size();
        int providedCount = parameters.size();
        
        if (providedCount < expectedCount) {
            throw new IllegalArgumentException(
                String.format("Se requieren %d parámetros posicionales, pero solo se proporcionaron %d", 
                    expectedCount, providedCount)
            );
        }
    }
    
    /**
     * Construye el contenido del mensaje para logging
     */
    private String buildTemplateContent(String templateName, Map<String, String> parameters, ParameterFormat format) {
        StringBuilder content = new StringBuilder();
        content.append("Template: ").append(templateName);
        
        if (!parameters.isEmpty()) {
            content.append(" | Parameters: ");
            if (format == ParameterFormat.NAMED) {
                parameters.forEach((key, value) -> 
                    content.append(key).append("=").append(value).append(", "));
            } else {
                parameters.values().forEach(value -> 
                    content.append(value).append(", "));
            }
            // Remover la última coma y espacio
            if (content.length() > 2) {
                content.setLength(content.length() - 2);
            }
        }
        
        return content.toString();
    }
    
    /**
     * Obtiene o crea una conversación
     */
    private UuidId<Conversation> getOrCreateConversation(
            UuidId<Client> clientId,
            UuidId<Contact> contactId,
            UuidId<ClientPhone> phoneId,
            UuidId<Conversation> existingConversationId
    ) {
        // Si se proporcionó una conversación existente, verificarla
        if (existingConversationId != null) {
            Optional<Conversation> existing = conversationRepository.findById(existingConversationId);
            if (existing.isPresent()) {
                return existingConversationId;
            }
        }
        
        // Crear nueva conversación
        // Nota: ConversationEntity requiere contactId obligatorio
        // Si contactId es null, no podemos crear una conversación válida
        if (contactId == null) {
            throw new IllegalStateException("No se puede crear una conversación sin un contacto. Proporcione contactId.");
        }
        
        Conversation newConversation = new Conversation(
                UuidId.newId(),
                clientId,
                contactId,
                phoneId,
                Channel.WHATSAPP,
                "Envío de plantilla",
                Instant.now()
        );
        
        conversationRepository.save(newConversation);
        return newConversation.id();
    }
    
    /**
     * Información del provider y número de origen
     */
    private record ProviderInfo(String provider, String fromNumber) {}
    
    /**
     * Determina el provider y el número de origen para enviar el mensaje usando la nueva arquitectura parametrizable
     * Lee la configuración desde la base de datos
     * 
     * @param phoneId ID del teléfono
     * @param phone El objeto ClientPhone
     * @return Información del provider y número de origen
     */
    private ProviderInfo determineProviderInfo(UuidId<ClientPhone> phoneId, ClientPhone phone) {
        String provider = phone.provider() != null ? phone.provider() : "META";
        
        // Obtener configuración del proveedor usando la nueva arquitectura
        Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> configOpt = 
                configServiceV2.getProviderConfiguration(phone.id(), provider);
        
        if (configOpt.isPresent()) {
            WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configOpt.get();
            
            // Para Meta, usar el phone_number_id si está disponible
            if ("META".equalsIgnoreCase(provider)) {
                String phoneNumberId = config.getConfigValueOrDefault("phone_number_id", "");
                if (!phoneNumberId.isEmpty()) {
                    log.debug("Usando phone_number_id desde configuración: {}", phoneNumberId);
                    return new ProviderInfo(provider, phoneNumberId);
                }
            }
            
            // Para otros proveedores, usar el provider_sid o el número E164
            String providerSid = phone.providerSidOpt().orElse(phone.phone().value());
            log.debug("Usando provider_sid o E164: {}", providerSid);
            return new ProviderInfo(provider, providerSid);
        }
        
        // Fallback a la lógica anterior si no hay configuración parametrizable
        log.warn("No se encontró configuración parametrizable para phone: {}. Usando fallback.", phoneId.value());
        
        // Para Meta, usar el provider_sid como phone_number_id
        if ("META".equalsIgnoreCase(provider) && phone.providerSidOpt().isPresent()) {
            return new ProviderInfo(provider, phone.providerSidOpt().get());
        }
        
        String fromNumber = phone.providerSidOpt().orElse(phone.phone().value());
        return new ProviderInfo(provider, fromNumber);
    }
    
    /**
     * Extrae el código de idioma desde el formato de la plantilla
     * Meta requiere el formato completo con código de país (ej: "en_US", "es_ES")
     * 
     * @param language Idioma de la plantilla desde la BD
     * @return Código de idioma para Meta API en formato ISO 639-1 + ISO 3166-1
     */
    private String extractLanguageCode(String language) {
        if (language == null || language.isEmpty()) {
            return "es_ES"; // Default a español con código de país para Meta
        }
        
        // Meta requiere el formato completo con código de país (ej: "en_US", "es_ES")
        // Si ya tiene el formato correcto, usar tal cual
        if (language.contains("_")) {
            return language;
        }
        
        // Si solo tiene el código de idioma sin país, normalizar
        return normalizeLanguageCode(language);
    }
    
    /**
     * Normaliza códigos de idioma simples a formato Meta con código de país
     */
    private String normalizeLanguageCode(String language) {
        return switch (language.toLowerCase()) {
            case "es" -> "es_ES";
            case "en" -> "en_US";
            case "pt" -> "pt_BR";
            case "fr" -> "fr_FR";
            case "de" -> "de_DE";
            case "it" -> "it_IT";
            default -> language + "_" + language.toUpperCase(); // Intento genérico
        };
    }
}