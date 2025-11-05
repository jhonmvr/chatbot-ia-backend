package com.relative.chat.bot.ia.infrastructure.adapters.in.web;

import com.relative.chat.bot.ia.application.dto.MessageCommand;
import com.relative.chat.bot.ia.application.usecases.ReceiveWhatsAppMessage;
import com.relative.chat.bot.ia.application.services.WhatsAppProviderConfigServiceV2;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.QualityRating;
import com.relative.chat.bot.ia.domain.messaging.TemplateStatus;
import com.relative.chat.bot.ia.domain.messaging.WhatsAppTemplate;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.WhatsAppTemplateRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Webhook unificado para recibir TODAS las notificaciones de Meta WhatsApp Business API
 * 
 * Según la documentación de Meta, solo se puede configurar UNA URL para webhooks.
 * Este endpoint maneja dinámicamente todos los tipos de eventos basándose en el campo 'field'.
 * 
 * Documentación: https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks
 * 
 * Eventos soportados:
 * - messages: Mensajes entrantes y estados de mensajes enviados
 * - message_template_status_update: Cambios de estado de templates
 * - message_template_quality_update: Cambios de calidad de templates
 * - message_template_components_update: Cambios en componentes de templates
 * - template_category_update: Cambios en categorías de templates
 * - account_alerts: Alertas de límites y cambios de perfil
 * - phone_number_quality_update: Cambios en límites de mensajería
 * - user_preferences: Preferencias de marketing de usuarios
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/whatsapp/meta")
@RequiredArgsConstructor
@Tag(name = "Meta WhatsApp Unified Webhook", description = "Webhook unificado para todos los eventos de Meta WhatsApp")
public class MetaWhatsAppUnifiedWebhookController {
    
    private final ReceiveWhatsAppMessage receiveWhatsAppMessage;
    private final ClientPhoneRepository clientPhoneRepository;
    private final WhatsAppProviderConfigServiceV2 configServiceV2;
    private final WhatsAppTemplateRepository templateRepository;
    
    /**
     * Endpoint de verificación del webhook (GET)
     * Meta lo llama al configurar el webhook
     */
    @Operation(
        summary = "Verificación del webhook unificado de Meta WhatsApp",
        description = "Endpoint usado por Meta para verificar el webhook durante la configuración. Retorna el challenge si el token es válido."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhook verificado exitosamente",
            content = @Content(mediaType = "text/plain")
        ),
        @ApiResponse(responseCode = "403", description = "Token de verificación inválido")
    })
    @GetMapping
    public ResponseEntity<?> verifyWebhook(
            @Parameter(description = "Modo de verificación (debe ser 'subscribe')", example = "subscribe")
            @RequestParam(value = "hub.mode", required = false) String mode,
            @Parameter(description = "Token de verificación configurado en Meta", example = "mi_token_secreto")
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @Parameter(description = "Challenge enviado por Meta", example = "1234567890")
            @RequestParam(value = "hub.challenge", required = false) String challenge
    ) {
        log.info("Verificación de webhook unificado recibida: mode={}, token={}", mode, token);
        
        if (!"subscribe".equals(mode)) {
            log.warn("Modo de verificación inválido: {}", mode);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }
        
        // Buscar configuración de Meta WhatsApp que tenga el token de verificación
        boolean tokenValid = clientPhoneRepository.findAll()
                .stream()
                .filter(phone -> "META".equalsIgnoreCase(phone.provider()))
                .anyMatch(phone -> {
                    // Verificar usando la nueva arquitectura parametrizable
                    Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> configOpt = 
                            configServiceV2.getProviderConfiguration(phone.id(), "META");
                    
                    if (configOpt.isPresent()) {
                        String verifyToken = configOpt.get().getConfigValueOrDefault("verify_token", "");
                        return token != null && token.equals(verifyToken);
                    }
                    
                    // Fallback a la lógica anterior si no hay configuración parametrizable
                    return token != null && token.equals(phone.webhookSecretOpt().orElse(null));
                });
        
        if (tokenValid) {
            log.info("Webhook unificado verificado exitosamente");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("Verificación de webhook fallida. Token recibido: {}", token);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }
    }
    
    /**
     * Endpoint unificado para recibir TODAS las notificaciones del webhook (POST)
     * 
     * Este endpoint maneja dinámicamente todos los tipos de eventos de Meta:
     * - messages: Mensajes entrantes y estados
     * - message_template_*: Eventos de templates
     * - account_*: Eventos de cuenta
     * - phone_number_*: Eventos de números de teléfono
     * - user_preferences: Preferencias de usuarios
     */
    @Operation(
        summary = "Webhook unificado de Meta WhatsApp",
        description = "Endpoint que recibe TODAS las notificaciones de Meta WhatsApp Business API y las procesa dinámicamente según el tipo de evento"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Notificación procesada correctamente"
        )
    })
    @PostMapping
    public ResponseEntity<?> handleUnifiedWebhook(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Payload de notificación de Meta WhatsApp",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "object": "whatsapp_business_account",
                      "entry": [
                        {
                          "id": "WHATSAPP_BUSINESS_ACCOUNT_ID",
                          "changes": [
                            {
                              "field": "messages",
                              "value": {
                                "messaging_product": "whatsapp",
                                "metadata": {
                                  "display_phone_number": "593987654321",
                                  "phone_number_id": "PHONE_NUMBER_ID"
                                },
                                "messages": [
                                  {
                                    "from": "593998765432",
                                    "id": "wamid.HBgLNTkzOTg3NjU0MzIyFQIAERgSOTkzOTE0NTY3ODkwMTIzNDUA",
                                    "timestamp": "1698765432",
                                    "type": "text",
                                    "text": {
                                      "body": "Hola, tengo una pregunta"
                                    }
                                  }
                                ]
                              }
                            }
                          ]
                        }
                      ]
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> payload
    ) {
        log.debug("Webhook unificado recibido: {}", payload);
        
        try {
            if (!"whatsapp_business_account".equals(payload.get("object"))) {
                log.warn("Tipo de objeto no soportado: {}", payload.get("object"));
                return ResponseEntity.ok().build();
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
            
            if (entries == null || entries.isEmpty()) {
                log.warn("No hay entries en el payload");
                return ResponseEntity.ok().build();
            }
            
            for (Map<String, Object> entry : entries) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                
                if (changes == null) continue;
                
                for (Map<String, Object> change : changes) {
                    String field = (String) change.get("field");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> value = (Map<String, Object>) change.get("value");
                    
                    log.info("Procesando evento de campo: {}", field);
                    
                    // Procesar dinámicamente según el tipo de evento
                    switch (field) {
                        case "messages" -> processMessagesEvent(value);
                        case "message_template_status_update" -> processTemplateStatusUpdate(value);
                        case "message_template_quality_update" -> processTemplateQualityUpdate(value);
                        case "message_template_components_update" -> processTemplateComponentsUpdate(value);
                        case "template_category_update" -> processTemplateCategoryUpdate(value);
                        case "account_alerts" -> processAccountAlerts(value);
                        case "account_review_update" -> processAccountReviewUpdate(value);
                        case "account_update" -> processAccountUpdate(value);
                        case "business_capability_update" -> processBusinessCapabilityUpdate(value);
                        case "phone_number_quality_update" -> processPhoneNumberQualityUpdate(value);
                        case "phone_number_name_update" -> processPhoneNumberNameUpdate(value);
                        case "user_preferences" -> processUserPreferences(value);
                        case "security" -> processSecurityEvent(value);
                        default -> {
                            log.info("Evento no manejado: {}", field);
                            // No retornamos error para eventos no manejados
                        }
                    }
                }
            }
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Error procesando webhook unificado de Meta: {}", e.getMessage(), e);
            // Retornar 200 de todas formas para que Meta no reintente
            return ResponseEntity.ok().build();
        }
    }
    
    // ==================== PROCESADORES DE EVENTOS ====================
    
    /**
     * Procesa eventos de mensajes (entrantes y estados)
     */
    @Hidden
    private void processMessagesEvent(Map<String, Object> value) {
        log.info("Procesando evento de mensajes");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
        
        if (messages != null && !messages.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) value.get("metadata");
            String phoneNumberId = metadata != null ? (String) metadata.get("phone_number_id") : null;
            
            for (Map<String, Object> message : messages) {
                try {
                    processMessage(message, phoneNumberId);
                } catch (Exception e) {
                    log.error("Error procesando mensaje individual: {}", e.getMessage(), e);
                }
            }
        }
        
        // También procesar estados de mensajes si están presentes
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> statuses = (List<Map<String, Object>>) value.get("statuses");
        if (statuses != null && !statuses.isEmpty()) {
            for (Map<String, Object> status : statuses) {
                try {
                    processMessageStatus(status);
                } catch (Exception e) {
                    log.error("Error procesando estado de mensaje: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Procesa eventos de actualización de estado de templates
     */
    @Hidden
    private void processTemplateStatusUpdate(Map<String, Object> value) {
        log.info("Procesando actualización de estado de template");
        
        try {
            String templateId = extractTemplateId(value);
            String newStatus = extractStatus(value);
            String rejectionReason = extractRejectionReason(value);
            
            if (templateId == null || newStatus == null) {
                log.warn("Datos incompletos para actualización de estado de template");
                return;
            }
            
            Optional<WhatsAppTemplate> templateOpt = templateRepository.findByMetaTemplateId(templateId);
            if (templateOpt.isEmpty()) {
                log.warn("Template no encontrado para Meta ID: {}", templateId);
                return;
            }
            
            WhatsAppTemplate template = templateOpt.get();
            TemplateStatus mappedStatus = mapMetaStatusToTemplateStatus(newStatus);
            
            WhatsAppTemplate updatedTemplate = template.withStatus(mappedStatus);
            
            if (mappedStatus == TemplateStatus.REJECTED && rejectionReason != null) {
                updatedTemplate = updatedTemplate.withRejectionReason(rejectionReason);
            } else if (mappedStatus == TemplateStatus.APPROVED) {
                updatedTemplate = updatedTemplate.withRejectionReason(null);
            }
            
            templateRepository.save(updatedTemplate);
            
            log.info("Estado de template actualizado: {} -> {} (Meta ID: {})", 
                template.id().value(), mappedStatus, templateId);
            
        } catch (Exception e) {
            log.error("Error procesando actualización de estado de template: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Procesa eventos de actualización de calidad de templates
     */
    @Hidden
    private void processTemplateQualityUpdate(Map<String, Object> value) {
        log.info("Procesando actualización de calidad de template");
        
        try {
            String templateId = extractTemplateId(value);
            String qualityScore = extractQualityScore(value);
            
            if (templateId == null || qualityScore == null) {
                log.warn("Datos incompletos para actualización de calidad de template");
                return;
            }
            
            Optional<WhatsAppTemplate> templateOpt = templateRepository.findByMetaTemplateId(templateId);
            if (templateOpt.isEmpty()) {
                log.warn("Template no encontrado para Meta ID: {}", templateId);
                return;
            }
            
            WhatsAppTemplate template = templateOpt.get();
            QualityRating mappedQuality = mapMetaQualityToQualityRating(qualityScore);
            
            WhatsAppTemplate updatedTemplate = template.withQualityRating(mappedQuality);
            templateRepository.save(updatedTemplate);
            
            log.info("Calidad de template actualizada: {} -> {} (Meta ID: {})", 
                template.id().value(), mappedQuality, templateId);
            
        } catch (Exception e) {
            log.error("Error procesando actualización de calidad de template: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Procesa eventos de actualización de componentes de templates
     */
    @Hidden
    private void processTemplateComponentsUpdate(Map<String, Object> value) {
        log.info("Procesando actualización de componentes de template");
        // TODO: Implementar lógica para actualizar componentes de templates
    }
    
    /**
     * Procesa eventos de actualización de categoría de templates
     */
    @Hidden
    private void processTemplateCategoryUpdate(Map<String, Object> value) {
        log.info("Procesando actualización de categoría de template");
        // TODO: Implementar lógica para actualizar categoría de templates
    }
    
    /**
     * Procesa alertas de cuenta
     */
    @Hidden
    private void processAccountAlerts(Map<String, Object> value) {
        log.info("Procesando alertas de cuenta");
        // TODO: Implementar lógica para alertas de cuenta (límites, cambios de perfil, etc.)
    }
    
    /**
     * Procesa actualizaciones de revisión de cuenta
     */
    @Hidden
    private void processAccountReviewUpdate(Map<String, Object> value) {
        log.info("Procesando actualización de revisión de cuenta");
        // TODO: Implementar lógica para actualizaciones de revisión
    }
    
    /**
     * Procesa actualizaciones de cuenta
     */
    @Hidden
    private void processAccountUpdate(Map<String, Object> value) {
        log.info("Procesando actualización de cuenta");
        // TODO: Implementar lógica para actualizaciones de cuenta
    }
    
    /**
     * Procesa actualizaciones de capacidades de negocio
     */
    @Hidden
    private void processBusinessCapabilityUpdate(Map<String, Object> value) {
        log.info("Procesando actualización de capacidades de negocio");
        // TODO: Implementar lógica para capacidades de negocio
    }
    
    /**
     * Procesa actualizaciones de calidad de número de teléfono
     */
    @Hidden
    private void processPhoneNumberQualityUpdate(Map<String, Object> value) {
        log.info("Procesando actualización de calidad de número de teléfono");
        // TODO: Implementar lógica para calidad de números de teléfono
    }
    
    /**
     * Procesa actualizaciones de nombre de número de teléfono
     */
    @Hidden
    private void processPhoneNumberNameUpdate(Map<String, Object> value) {
        log.info("Procesando actualización de nombre de número de teléfono");
        // TODO: Implementar lógica para nombres de números de teléfono
    }
    
    /**
     * Procesa preferencias de usuario
     */
    @Hidden
    private void processUserPreferences(Map<String, Object> value) {
        log.info("Procesando preferencias de usuario");
        // TODO: Implementar lógica para preferencias de marketing de usuarios
    }
    
    /**
     * Procesa eventos de seguridad
     */
    @Hidden
    private void processSecurityEvent(Map<String, Object> value) {
        log.info("Procesando evento de seguridad");
        // TODO: Implementar lógica para eventos de seguridad
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Procesa un mensaje individual
     */
    @Hidden
    private void processMessage(Map<String, Object> message, String phoneNumberId) {
        String messageId = (String) message.get("id");
        String from = (String) message.get("from");
        String type = (String) message.get("type");
        
        log.info("Mensaje recibido - ID: {}, From: {}, Type: {}, PhoneNumberId: {}", 
                 messageId, from, type, phoneNumberId);
        
        // Por ahora solo procesamos mensajes de texto
        if (!"text".equals(type)) {
            log.info("Tipo de mensaje no soportado: {}", type);
            return;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) message.get("text");
        if (text == null) {
            log.warn("Mensaje de texto sin contenido");
            return;
        }
        
        String body = (String) text.get("body");
        
        if (body == null || body.isBlank()) {
            log.warn("Mensaje vacío");
            return;
        }
        
        // Resolver el cliente por el phoneNumberId de Meta
        Optional<Client> clientOpt = resolveClient(phoneNumberId);
        
        if (clientOpt.isEmpty()) {
            log.error("❌ No se pudo identificar el cliente para phoneNumberId: {}. " +
                      "Verifica que exista un registro en client_phone con provider_sid='{}' y provider='META'",
                      phoneNumberId, phoneNumberId);
            return;
        }
        
        Client client = clientOpt.get();
        log.info("✅ Cliente identificado: {} (code: {})", client.name(), client.code());
        
        // Obtener nombre del contacto si está disponible
        String displayName = from; // Por defecto, usar el número
        
        try {
            // Crear comando de mensaje con el clientCode resuelto
            MessageCommand command = new MessageCommand(
                    client.code(), // clientCode resuelto desde la BD
                    phoneNumberId, // phoneNumber - nuestro número de WhatsApp Business (phone_number_id de Meta)
                    from,          // contactPhone - número del usuario que escribe
                    displayName,   // contactName
                    Channel.WHATSAPP,
                    body,          // content
                    Instant.now(),
                    messageId      // externalId
            );
            
            // Procesar el mensaje
            receiveWhatsAppMessage.handle(command);
            
            log.info("✅ Mensaje procesado exitosamente: {}", messageId);
            
        } catch (Exception e) {
            log.error("❌ Error procesando mensaje {}: {}", messageId, e.getMessage(), e);
        }
    }
    
    /**
     * Procesa estados de mensajes enviados
     */
    @Hidden
    private void processMessageStatus(Map<String, Object> status) {
        String messageId = (String) status.get("id");
        String statusValue = (String) status.get("status");
        String recipientId = (String) status.get("recipient_id");
        
        log.info("Estado de mensaje recibido - ID: {}, Status: {}, Recipient: {}", 
                 messageId, statusValue, recipientId);
        
        // TODO: Implementar lógica para actualizar estados de mensajes en la BD
        // Esto podría incluir marcar mensajes como entregados, leídos, fallidos, etc.
    }
    
    /**
     * Resuelve el cliente basándose en el phone_number_id de Meta WhatsApp
     */
    @Hidden
    private Optional<Client> resolveClient(String phoneNumberId) {
        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            log.warn("phoneNumberId es null o vacío");
            return Optional.empty();
        }
        
        log.debug("Buscando cliente con phoneNumberId: {}", phoneNumberId);
        
        // Buscar en la tabla client_phone por provider_sid='phoneNumberId' y provider='META'
        return clientPhoneRepository.findClientByProviderSid(phoneNumberId, "META");
    }
    
    // ==================== MÉTODOS DE EXTRACCIÓN ====================
    
    private String extractTemplateId(Map<String, Object> value) {
        try {
            return (String) value.get("id");
        } catch (Exception e) {
            log.warn("Error al extraer template ID: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractStatus(Map<String, Object> value) {
        try {
            return (String) value.get("status");
        } catch (Exception e) {
            log.warn("Error al extraer status: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractQualityScore(Map<String, Object> value) {
        try {
            return (String) value.get("quality_score");
        } catch (Exception e) {
            log.warn("Error al extraer quality_score: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractRejectionReason(Map<String, Object> value) {
        try {
            return (String) value.get("rejection_reason");
        } catch (Exception e) {
            log.warn("Error al extraer rejection_reason: {}", e.getMessage());
            return null;
        }
    }
    
    // ==================== MÉTODOS DE MAPEO ====================
    
    private TemplateStatus mapMetaStatusToTemplateStatus(String metaStatus) {
        if (metaStatus == null) return TemplateStatus.PENDING;
        
        return switch (metaStatus.toUpperCase()) {
            case "APPROVED" -> TemplateStatus.APPROVED;
            case "PENDING" -> TemplateStatus.PENDING;
            case "REJECTED" -> TemplateStatus.REJECTED;
            case "PAUSED" -> TemplateStatus.PAUSED;
            case "DISABLED" -> TemplateStatus.DISABLED;
            default -> TemplateStatus.PENDING;
        };
    }
    
    private QualityRating mapMetaQualityToQualityRating(String metaQuality) {
        if (metaQuality == null) return QualityRating.PENDING;
        
        return switch (metaQuality.toUpperCase()) {
            case "HIGH" -> QualityRating.HIGH;
            case "MEDIUM" -> QualityRating.MEDIUM;
            case "LOW" -> QualityRating.LOW;
            default -> QualityRating.PENDING;
        };
    }
}
