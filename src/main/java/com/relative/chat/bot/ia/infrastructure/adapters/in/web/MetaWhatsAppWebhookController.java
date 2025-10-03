package com.relative.chat.bot.ia.infrastructure.adapters.in.web;

import com.relative.chat.bot.ia.application.dto.MessageCommand;
import com.relative.chat.bot.ia.application.usecases.ReceiveWhatsAppMessage;
import com.relative.chat.bot.ia.domain.types.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Webhook para recibir notificaciones de Meta WhatsApp Business API
 * 
 * Documentación: https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/whatsapp/meta")
@RequiredArgsConstructor
public class MetaWhatsAppWebhookController {
    
    private final ReceiveWhatsAppMessage receiveWhatsAppMessage;
    
    @Value("${app.whatsapp.meta.webhook-verify-token:}")
    private String webhookVerifyToken;
    
    /**
     * Endpoint de verificación del webhook (GET)
     * Meta lo llama al configurar el webhook
     */
    @GetMapping
    public ResponseEntity<?> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge
    ) {
        log.info("Verificación de webhook recibida: mode={}, token={}", mode, token);
        
        if ("subscribe".equals(mode) && webhookVerifyToken.equals(token)) {
            log.info("Webhook verificado exitosamente");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("Verificación de webhook fallida. Token esperado: {}, recibido: {}", 
                    webhookVerifyToken, token);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }
    }
    
    /**
     * Endpoint para recibir notificaciones del webhook (POST)
     */
    @PostMapping
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {
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
    private void processMessage(Map<String, Object> message, String phoneNumberId) {
        String messageId = (String) message.get("id");
        String from = (String) message.get("from");
        String type = (String) message.get("type");
        
        log.info("Mensaje recibido - ID: {}, From: {}, Type: {}", messageId, from, type);
        
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
        
        // Obtener nombre del contacto si está disponible
        String displayName = from; // Por defecto, usar el número
        
        try {
            // Crear comando de mensaje con la estructura correcta
            MessageCommand command = new MessageCommand(
                    null,          // clientCode - se resolverá por el phoneNumberId
                    phoneNumberId, // phoneNumber - nuestro número de WhatsApp Business
                    from,          // contactPhone - número del usuario que escribe
                    displayName,   // contactName
                    Channel.WHATSAPP,
                    body,          // content
                    Instant.now(),
                    messageId      // externalId
            );
            
            // Procesar el mensaje
            receiveWhatsAppMessage.handle(command);
            
            log.info("Mensaje procesado exitosamente: {}", messageId);
            
        } catch (Exception e) {
            log.error("Error procesando mensaje {}: {}", messageId, e.getMessage(), e);
        }
    }
}
