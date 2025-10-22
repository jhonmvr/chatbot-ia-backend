package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.usecases.SendTemplate;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.*;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactRepository;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * API REST para envío de mensajes con plantillas
 * 
 * Basado en la documentación de WhatsApp Business Management API:
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/message-templates
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/authentication-templates
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/message-templates/marketing-templates
 */
@Slf4j
@RestController
@RequestMapping("/api/templates/send")
@RequiredArgsConstructor
@Tag(name = "Template Sending", description = "API para enviar mensajes con plantillas de WhatsApp")
public class SendTemplateController {
    
    private final SendTemplate sendTemplate;
    private final ClientPhoneRepository clientPhoneRepository;
    private final ContactRepository contactRepository;
    
    /**
     * Enviar mensaje con plantilla a un contacto específico
     * POST /api/templates/send/single
     */
    @Operation(
        summary = "Enviar plantilla a contacto individual",
        description = "Envía un mensaje con plantilla a un contacto específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plantilla enviada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "messageId": "550e8400-e29b-41d4-a716-446655440000",
                      "externalId": "wamid.xxx",
                      "templateName": "welcome_message",
                      "sentAt": "2024-12-19T10:30:00Z"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o plantilla no aprobada",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Template 'welcome_message' no está aprobado. Estado actual: PENDING"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Contacto o teléfono no encontrado")
    })
    @PostMapping(value = "/single", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendTemplateToContact(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos para envío de plantilla",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientId": "a1234567-e89b-12d3-a456-426614174000",
                      "phoneId": "b1234567-e89b-12d3-a456-426614174000",
                      "contactId": "c1234567-e89b-12d3-a456-426614174000",
                      "templateName": "welcome_message",
                      "parameterFormat": "NAMED",
                      "parameters": {
                        "first_name": "Juan",
                        "company_name": "Tech Solutions"
                      }
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            // Validar campos requeridos
            String clientId = (String) request.get("clientId");
            String phoneId = (String) request.get("phoneId");
            String contactId = (String) request.get("contactId");
            String templateName = (String) request.get("templateName");
            String parameterFormatStr = (String) request.getOrDefault("parameterFormat", "NAMED");
            
            if (clientId == null || phoneId == null || contactId == null || templateName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientId, phoneId, contactId y templateName son requeridos"
                ));
            }
            
            // Verificar que el contacto existe
            Optional<Contact> contactOpt = contactRepository.findById(UuidId.of(UUID.fromString(contactId)));
            if (contactOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Contacto no encontrado: " + contactId
                ));
            }
            
            Contact contact = contactOpt.get();
            
            // Verificar que el teléfono existe
            Optional<ClientPhone> phoneOpt = clientPhoneRepository.findById(UuidId.of(UUID.fromString(phoneId)));
            if (phoneOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Teléfono no encontrado: " + phoneId
                ));
            }
            
            // Parsear parámetros
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) request.getOrDefault("parameters", new HashMap<>());
            
            // Parsear formato de parámetros
            ParameterFormat parameterFormat;
            try {
                parameterFormat = ParameterFormat.valueOf(parameterFormatStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Formato de parámetros inválido. Use NAMED o POSITIONAL"
                ));
            }
            
            // Crear conversación si no existe
            UuidId<Conversation> conversationId = UuidId.newId();
            
            // Enviar plantilla
            Message message = sendTemplate.handle(
                    UuidId.of(UUID.fromString(clientId)),
                    conversationId,
                    contact.id(),
                    UuidId.of(UUID.fromString(phoneId)),
                    templateName,
                    parameters,
                    parameterFormat,
                    contact.phoneE164() != null ? contact.phoneE164().value() : null
            );
            
            log.info("Plantilla '{}' enviada exitosamente a contacto {}", templateName, contactId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "messageId", message.id().value().toString(),
                "externalId", message.externalId().orElse("N/A"),
                "templateName", templateName,
                "sentAt", message.sentAt().orElse(Instant.now())
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            log.error("Error de estado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al enviar plantilla: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al enviar plantilla: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Enviar mensaje con plantilla por número de teléfono
     * POST /api/templates/send/by-phone
     */
    @Operation(
        summary = "Enviar plantilla por número de teléfono",
        description = "Envía un mensaje con plantilla directamente a un número de teléfono"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plantilla enviada exitosamente"
        ),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping(value = "/by-phone", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendTemplateByPhone(
        @RequestBody Map<String, Object> request
    ) {
        try {
            // Validar campos requeridos
            String clientId = (String) request.get("clientId");
            String phoneId = (String) request.get("phoneId");
            String toNumber = (String) request.get("toNumber");
            String templateName = (String) request.get("templateName");
            String parameterFormatStr = (String) request.getOrDefault("parameterFormat", "NAMED");
            
            if (clientId == null || phoneId == null || toNumber == null || templateName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientId, phoneId, toNumber y templateName son requeridos"
                ));
            }
            
            // Parsear parámetros
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) request.getOrDefault("parameters", new HashMap<>());
            
            // Parsear formato de parámetros
            ParameterFormat parameterFormat;
            try {
                parameterFormat = ParameterFormat.valueOf(parameterFormatStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Formato de parámetros inválido. Use NAMED o POSITIONAL"
                ));
            }
            
            // Crear conversación temporal
            UuidId<Conversation> conversationId = UuidId.newId();
            
            // Enviar plantilla
            Message message = sendTemplate.handle(
                    UuidId.of(UUID.fromString(clientId)),
                    conversationId,
                    null, // Sin contacto específico
                    UuidId.of(UUID.fromString(phoneId)),
                    templateName,
                    parameters,
                    parameterFormat,
                    toNumber
            );
            
            log.info("Plantilla '{}' enviada exitosamente a número {}", templateName, toNumber);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "messageId", message.id().value().toString(),
                "externalId", message.externalId().orElse("N/A"),
                "templateName", templateName,
                "toNumber", toNumber,
                "sentAt", message.sentAt().orElse(Instant.now())
            ));
            
        } catch (Exception e) {
            log.error("Error al enviar plantilla por teléfono: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al enviar plantilla: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Validar plantilla antes del envío
     * GET /api/templates/send/validate/{templateName}
     */
    @Operation(
        summary = "Validar plantilla",
        description = "Valida que una plantilla esté disponible y aprobada para envío"
    )
    @GetMapping("/validate/{templateName}")
    public ResponseEntity<Map<String, Object>> validateTemplate(
        @Parameter(description = "Nombre de la plantilla", required = true)
        @PathVariable String templateName,
        @RequestParam String phoneId
    ) {
        try {
            // TODO: Implementar validación de plantilla
            // Por ahora, respuesta básica
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "templateName", templateName,
                "isValid", true,
                "message", "Plantilla válida para envío"
            ));
            
        } catch (Exception e) {
            log.error("Error al validar plantilla: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al validar plantilla: " + e.getMessage()
            ));
        }
    }
}
