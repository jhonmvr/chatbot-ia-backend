package com.relative.chat.bot.ia.infrastructure.adapters.in.web;

import com.relative.chat.bot.ia.application.dto.MessageCommand;
import com.relative.chat.bot.ia.application.usecases.ReceiveWhatsAppMessage;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
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
 * Webhook para recibir notificaciones de Meta WhatsApp Business API
 * 
 * Documentación: https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/whatsapp/meta")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Endpoints para recibir notificaciones de servicios externos")
public class MetaWhatsAppWebhookController {
    
    private final ReceiveWhatsAppMessage receiveWhatsAppMessage;
    private final ClientPhoneRepository clientPhoneRepository;
    
    /**
     * Endpoint de verificación del webhook (GET)
     * Meta lo llama al configurar el webhook
     */
    @Operation(
        summary = "Verificación del webhook de Meta WhatsApp",
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
        log.info("Verificación de webhook recibida: mode={}, token={}", mode, token);
        
        if (!"subscribe".equals(mode)) {
            log.warn("Modo de verificación inválido: {}", mode);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }
        
        // Buscar configuración de Meta WhatsApp que tenga el token de verificación
        boolean tokenValid = clientPhoneRepository.findAll()
                .stream()
                .filter(phone -> "META".equalsIgnoreCase(phone.provider()))
                .anyMatch(phone -> token != null && token.equals(phone.verifyTokenOpt().orElse(null)));
        
        if (tokenValid) {
            log.info("Webhook verificado exitosamente");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("Verificación de webhook fallida. Token recibido: {}", token);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }
    }
    
    /**
     * Endpoint para recibir notificaciones del webhook (POST)
     */
    @Operation(
        summary = "Recibir notificaciones de Meta WhatsApp",
        description = "Endpoint que recibe las notificaciones de mensajes entrantes desde Meta WhatsApp Business API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Notificación procesada correctamente"
        )
    })
    @PostMapping
    public ResponseEntity<?> handleWebhook(
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
        log.debug("Webhook recibido: {}", payload);
        
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
                    
                    if (!"messages".equals(field)) {
                        log.debug("Campo ignorado: {}", field);
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> value = (Map<String, Object>) change.get("value");
                    
                    if (value != null) {
                        processMessages(value);
                    }
                }
            }
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Error procesando webhook de Meta: {}", e.getMessage(), e);
            // Retornar 200 de todas formas para que Meta no reintente
            return ResponseEntity.ok().build();
        }
    }
    
    /**
     * Procesa los mensajes recibidos
     */
    @Hidden
    private void processMessages(Map<String, Object> value) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
        
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
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
        
        // 🔑 CLAVE: Resolver el cliente por el phoneNumberId de Meta
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
                    client.code(), // ✅ clientCode resuelto desde la BD
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
     * Resuelve el cliente basándose en el phone_number_id de Meta WhatsApp
     * 
     * El phoneNumberId es el identificador único que Meta asigna a cada número de WhatsApp Business.
     * Este valor debe estar almacenado en la columna provider_sid de la tabla client_phone.
     * 
     * @param phoneNumberId El phone_number_id de Meta
     * @return El cliente dueño de ese número de WhatsApp, o Optional.empty() si no se encuentra
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
}
