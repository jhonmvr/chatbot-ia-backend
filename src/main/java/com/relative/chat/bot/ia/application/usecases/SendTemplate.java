package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.ports.out.WhatsAppService;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.*;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
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
    private final WhatsAppService whatsAppService;
    private final ClientPhoneRepository clientPhoneRepository;
    private final WhatsAppTemplateRepository templateRepository;
    
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
        
        // 6. Crear el mensaje
        Message message = new Message(
                UuidId.newId(),
                clientId,
                conversationId,
                contactId,
                phoneId,
                Channel.WHATSAPP,
                Direction.OUT,
                buildTemplateContent(templateName, parameters, parameterFormat),
                Instant.now()
        );
        
        try {
            // 7. Enviar a través del servicio externo
            if (whatsAppService != null) {
                String externalId = whatsAppService.sendTemplate(
                    phone.phone().value(), 
                    toNumber, 
                    templateName, 
                    parameters
                );
                message.markSent(Instant.now(), externalId);
                log.info("Plantilla '{}' enviada exitosamente. ID externo: {}", templateName, externalId);
            }
        } catch (Exception e) {
            log.error("Error al enviar plantilla '{}': {}", templateName, e.getMessage(), e);
            message.fail(e.getMessage());
        }
        
        // 8. Guardar el mensaje
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
}