package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.dto.MessageCommand;
import com.relative.chat.bot.ia.application.dto.MessageResponse;
import com.relative.chat.bot.ia.application.usecases.ReceiveWhatsAppMessage;
import com.relative.chat.bot.ia.domain.types.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Controlador REST para webhook de WhatsApp
 */
@Slf4j
@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {
    
    private final ReceiveWhatsAppMessage receiveWhatsAppMessage;
    
    /**
     * Endpoint para verificación del webhook (GET)
     * Usado por WhatsApp/Twilio para verificar el webhook
     */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        log.info("Verificación de webhook: mode={}, token={}", mode, token);
        
        // TODO: Validar el token con una configuración
        if ("subscribe".equals(mode) && challenge != null) {
            log.info("Webhook verificado exitosamente");
            return ResponseEntity.ok(challenge);
        }
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verificación fallida");
    }
    
    /**
     * Endpoint para recibir mensajes entrantes (POST)
     * Procesa mensajes de WhatsApp a través de Twilio u otro proveedor
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> receive(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Webhook recibido: {}", payload);
            
            // Extraer datos del payload
            // El formato puede variar según el proveedor (Twilio, Meta, etc.)
            MessageCommand command = parsePayload(payload);
            
            // Procesar el mensaje
            MessageResponse response = receiveWhatsAppMessage.handle(command);
            
            if (response.success()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error al procesar webhook: {}", e.getMessage(), e);
            MessageResponse errorResponse = MessageResponse.error("Error al procesar mensaje: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Parsea el payload del webhook según el formato del proveedor
     */
    private MessageCommand parsePayload(Map<String, Object> payload) {
        // Formato genérico - adaptar según el proveedor real
        String clientCode = (String) payload.getOrDefault("clientCode", "default-client");
        String phoneNumber = (String) payload.getOrDefault("phoneNumber", "+593999999999");
        String contactPhone = (String) payload.getOrDefault("from", payload.get("From"));
        String contactName = (String) payload.getOrDefault("profileName", payload.get("ProfileName"));
        String content = (String) payload.getOrDefault("text", payload.get("Body"));
        String externalId = (String) payload.getOrDefault("messageId", payload.get("MessageSid"));
        
        // Si no hay contenido, usar mensaje por defecto
        if (content == null || content.isBlank()) {
            content = "hola";
        }
        
        return new MessageCommand(
                clientCode,
                phoneNumber,
                contactPhone,
                contactName,
                Channel.WHATSAPP,
                content,
                Instant.now(),
                externalId
        );
    }
    
    /**
     * Endpoint simple para probar el webhook
     */
    @PostMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> test(@RequestBody(required = false) Map<String, Object> body) {
        String message = body != null && body.containsKey("message") 
                ? (String) body.get("message") 
                : "hola";
        
        MessageCommand command = new MessageCommand(
                "test-client",
                "+593999999999",
                "+593988888888",
                "Usuario Test",
                Channel.WHATSAPP,
                message,
                Instant.now(),
                null
        );
        
        MessageResponse response = receiveWhatsAppMessage.handle(command);
        
        return ResponseEntity.ok(Map.of(
                "success", response.success(),
                "response", response.response() != null ? response.response() : "Sin respuesta",
                "conversationId", response.conversationId() != null ? response.conversationId().toString() : "N/A"
        ));
    }
}
