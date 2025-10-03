package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.usecases.CloseConversation;
import com.relative.chat.bot.ia.application.usecases.GetConversationHistory;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * API REST para gestión de conversaciones
 */
@Slf4j
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {
    
    private final ConversationRepository conversationRepository;
    private final GetConversationHistory getConversationHistory;
    private final CloseConversation closeConversation;
    
    /**
     * Obtiene una conversación específica con su historial
     * GET /api/conversations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable String id) {
        try {
            UuidId<Conversation> conversationId = UuidId.of(UUID.fromString(id));
            
            Optional<Conversation> conversation = conversationRepository.findById(conversationId);
            
            if (conversation.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Conversation conv = conversation.get();
            
            // Obtener historial de mensajes
            List<Message> messages = getConversationHistory.handle(conversationId, 100);
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", conv.id().value().toString());
            result.put("clientId", conv.clientId().value().toString());
            result.put("contactId", conv.contactId().map(cId -> cId.value().toString()).orElse(null));
            result.put("status", conv.status().name());
            result.put("startedAt", conv.startedAt());
            result.put("closedAt", conv.closedAt().orElse(null));
            result.put("messageCount", messages.size());
            result.put("messages", messages.stream()
                    .map(msg -> Map.of(
                            "id", msg.id().value().toString(),
                            "direction", msg.direction().name(),
                            "content", msg.content(),
                            "createdAt", msg.createdAt()
                    ))
                    .toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al obtener conversación: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Cierra una conversación
     * POST /api/conversations/{id}/close
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<Map<String, String>> closeConversation(@PathVariable String id) {
        try {
            UuidId<Conversation> conversationId = UuidId.of(UUID.fromString(id));
            
            closeConversation.handle(conversationId);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Conversación cerrada exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error al cerrar conversación: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}
