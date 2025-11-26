package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.usecases.CloseConversation;
import com.relative.chat.bot.ia.application.usecases.GetConversationHistory;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
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

import java.util.*;

/**
 * API REST para gestión de conversaciones
 */
@Slf4j
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversaciones", description = "API para gestionar conversaciones del chatbot con clientes")
public class ConversationController {
    
    private final ConversationRepository conversationRepository;
    private final GetConversationHistory getConversationHistory;
    private final CloseConversation closeConversation;
    
    /**
     * Obtiene una conversación específica con su historial
     * GET /api/conversations/{id}
     */
    @Operation(
        summary = "Obtener conversación con historial completo",
        description = "Retorna los detalles de una conversación incluyendo todos sus mensajes ordenados cronológicamente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversación encontrada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "id": "550e8400-e29b-41d4-a716-446655440000",
                      "clientId": "123e4567-e89b-12d3-a456-426614174000",
                      "contactId": "987fbc97-4bed-5078-9f07-9141ba07c9f3",
                      "status": "OPEN",
                      "startedAt": "2025-10-03T10:30:00Z",
                      "closedAt": null,
                      "messageCount": 5,
                      "messages": [
                        {
                          "id": "msg-001",
                          "direction": "INBOUND",
                          "content": "Hola, tengo una consulta",
                          "createdAt": "2025-10-03T10:30:00Z"
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "ID de conversación inválido",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "ID de conversación inválido: formato UUID incorrecto"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Conversación no encontrada",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Conversación no encontrada"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getConversation(
        @Parameter(description = "UUID de la conversación", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
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
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "ID de conversación inválido: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener conversación: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener conversación: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Cierra una conversación
     * POST /api/conversations/{id}/close
     */
    @Operation(
        summary = "Cerrar una conversación",
        description = "Marca una conversación como cerrada. Una vez cerrada, no se podrán enviar más mensajes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversación cerrada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "message": "Conversación cerrada exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Error al cerrar la conversación",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "La conversación ya está cerrada"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Conversación no encontrada")
    })
    @PostMapping("/{id}/close")
    public ResponseEntity<Map<String, String>> closeConversation(
        @Parameter(description = "UUID de la conversación", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
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
