package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.usecases.CloseConversation;
import com.relative.chat.bot.ia.application.usecases.GetConversationHistory;
import com.relative.chat.bot.ia.application.usecases.GetContactConversations;
import com.relative.chat.bot.ia.application.usecases.GetContactsWithConversations;
import com.relative.chat.bot.ia.application.usecases.ListConversations;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
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
    private final ListConversations listConversations;
    private final GetContactConversations getContactConversations;
    private final GetContactsWithConversations getContactsWithConversations;
    private final ContactRepository contactRepository;
    
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
    
    /**
     * Lista todas las conversaciones con búsqueda y filtros
     * GET /api/conversations
     */
    @Operation(
        summary = "Listar conversaciones con búsqueda",
        description = "Lista todas las conversaciones con capacidad de búsqueda en títulos, mensajes y contactos. " +
                     "Funciona como una lista de chats donde cada conversación muestra información del contacto y último mensaje."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de conversaciones obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "conversations": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "clientId": "123e4567-e89b-12d3-a456-426614174000",
                          "contactId": "987fbc97-4bed-5078-9f07-9141ba07c9f3",
                          "contact": {
                            "id": "987fbc97-4bed-5078-9f07-9141ba07c9f3",
                            "displayName": "Juan Pérez",
                            "phone": "+593991234567",
                            "email": "juan@example.com"
                          },
                          "status": "OPEN",
                          "channel": "WHATSAPP",
                          "title": "Consulta sobre producto",
                          "startedAt": "2025-10-03T10:30:00Z",
                          "closedAt": null,
                          "lastMessage": {
                            "id": "msg-001",
                            "content": "Gracias por su respuesta",
                            "direction": "INBOUND",
                            "createdAt": "2025-10-03T11:00:00Z"
                          },
                          "messageCount": 5
                        }
                      ],
                      "total": 150,
                      "page": 0,
                      "size": 20,
                      "totalPages": 8
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> listConversations(
        @Parameter(description = "Texto de búsqueda (busca en títulos, mensajes y contactos)")
        @RequestParam(required = false) String query,
        
        @Parameter(description = "UUID del cliente para filtrar")
        @RequestParam(required = false) String clientId,
        
        @Parameter(description = "UUID del contacto para filtrar")
        @RequestParam(required = false) String contactId,
        
        @Parameter(description = "Estado de la conversación (OPEN, CLOSED)")
        @RequestParam(required = false) String status,
        
        @Parameter(description = "Canal de comunicación (WHATSAPP, SMS, etc.)")
        @RequestParam(required = false) String channel,
        
        @Parameter(description = "Número de página (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Tamaño de página", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            UuidId<Client> clientUuidId = clientId != null ? UuidId.of(UUID.fromString(clientId)) : null;
            UuidId<Contact> contactUuidId = contactId != null ? UuidId.of(UUID.fromString(contactId)) : null;
            Channel channelEnum = channel != null && !channel.isEmpty() ? Channel.valueOf(channel) : null;
            
            ConversationRepository.SearchResult result = listConversations.handle(
                clientUuidId,
                query,
                contactUuidId,
                status,
                channelEnum,
                page,
                size
            );
            
            // Enriquecer con información de contacto y último mensaje
            List<Map<String, Object>> conversationDtos = result.conversations().stream()
                .map(conv -> toConversationListItemDto(conv))
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "conversations", conversationDtos,
                "total", result.total(),
                "page", result.page(),
                "size", result.size(),
                "totalPages", result.totalPages()
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Parámetro inválido: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al listar conversaciones: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al listar conversaciones: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtiene todas las conversaciones de un contacto específico
     * GET /api/conversations/contact/{contactId}
     */
    @Operation(
        summary = "Obtener conversaciones de un contacto",
        description = "Retorna todas las conversaciones de un contacto específico, ordenadas por fecha de inicio (más recientes primero)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversaciones del contacto obtenidas exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "contact": {
                        "id": "987fbc97-4bed-5078-9f07-9141ba07c9f3",
                        "displayName": "Juan Pérez",
                        "phone": "+593991234567",
                        "email": "juan@example.com"
                      },
                      "conversations": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "status": "OPEN",
                          "channel": "WHATSAPP",
                          "title": "Consulta sobre producto",
                          "startedAt": "2025-10-03T10:30:00Z",
                          "messageCount": 5
                        }
                      ],
                      "total": 10,
                      "page": 0,
                      "size": 20,
                      "totalPages": 1
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Contacto no encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Contacto no encontrado"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/contact/{contactId}")
    public ResponseEntity<Map<String, Object>> getContactConversations(
        @Parameter(description = "UUID del contacto", required = true, example = "987fbc97-4bed-5078-9f07-9141ba07c9f3")
        @PathVariable String contactId,
        
        @Parameter(description = "Número de página (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Tamaño de página", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            UuidId<Contact> contactUuidId = UuidId.of(UUID.fromString(contactId));
            
            // Verificar que el contacto existe
            Optional<Contact> contactOpt = contactRepository.findById(contactUuidId);
            if (contactOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Contacto no encontrado"
                ));
            }
            
            Contact contact = contactOpt.get();
            
            // Obtener conversaciones del contacto
            ConversationRepository.SearchResult result = getContactConversations.handle(
                contactUuidId,
                page,
                size
            );
            
            // Convertir a DTOs
            List<Map<String, Object>> conversationDtos = result.conversations().stream()
                .map(conv -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", conv.id().value().toString());
                    dto.put("clientId", conv.clientId().value().toString());
                    dto.put("status", conv.status().name());
                    dto.put("channel", conv.channel().name());
                    dto.put("title", conv.title() != null ? conv.title() : "");
                    dto.put("startedAt", conv.startedAt());
                    dto.put("closedAt", conv.closedAt().orElse(null));
                    dto.put("messageCount", getConversationHistory.handle(conv.id(), 1).size());
                    return dto;
                })
                .toList();
            
            // Información básica del contacto
            Map<String, Object> contactDto = new HashMap<>();
            contactDto.put("id", contact.id().value().toString());
            contactDto.put("displayName", contact.displayName() != null ? contact.displayName() : "");
            contactDto.put("phone", contact.phoneE164() != null ? contact.phoneE164().value() : null);
            contactDto.put("email", contact.email() != null ? contact.email().value() : null);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "contact", contactDto,
                "conversations", conversationDtos,
                "total", result.total(),
                "page", result.page(),
                "size", result.size(),
                "totalPages", result.totalPages()
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "ID de contacto inválido: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener conversaciones del contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener conversaciones del contacto: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Convierte una conversación a DTO para lista de chats
     * Incluye información del contacto y último mensaje
     */
    private Map<String, Object> toConversationListItemDto(Conversation conv) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", conv.id().value().toString());
        dto.put("clientId", conv.clientId().value().toString());
        dto.put("contactId", conv.contactId().map(cId -> cId.value().toString()).orElse(null));
        dto.put("status", conv.status().name());
        dto.put("channel", conv.channel().name());
        dto.put("title", conv.title() != null ? conv.title() : "");
        dto.put("startedAt", conv.startedAt());
        dto.put("closedAt", conv.closedAt().orElse(null));
        
        // Información del contacto
        Map<String, Object> contactDto = new HashMap<>();
        conv.contactId().ifPresent(contactId -> {
            contactRepository.findById(contactId).ifPresent(contact -> {
                contactDto.put("id", contact.id().value().toString());
                contactDto.put("displayName", contact.displayName() != null ? contact.displayName() : "");
                contactDto.put("phone", contact.phoneE164() != null ? contact.phoneE164().value() : null);
                contactDto.put("email", contact.email() != null ? contact.email().value() : null);
            });
        });
        dto.put("contact", contactDto.isEmpty() ? null : contactDto);
        
        // Último mensaje
        List<Message> messages = getConversationHistory.handle(conv.id(), 1);
        if (!messages.isEmpty()) {
            Message lastMessage = messages.get(0);
            Map<String, Object> lastMessageDto = Map.of(
                "id", lastMessage.id().value().toString(),
                "content", lastMessage.content() != null ? lastMessage.content() : "",
                "direction", lastMessage.direction().name(),
                "createdAt", lastMessage.createdAt()
            );
            dto.put("lastMessage", lastMessageDto);
        } else {
            dto.put("lastMessage", null);
        }
        
        // Contador de mensajes
        List<Message> allMessages = getConversationHistory.handle(conv.id(), 100);
        dto.put("messageCount", allMessages.size());
        
        return dto;
    }
    
    /**
     * Lista contactos únicos que tengan conversaciones, ordenados por mensaje más reciente
     * GET /api/conversations/contacts
     */
    @Operation(
        summary = "Listar contactos con conversaciones",
        description = "Retorna una lista paginada de contactos únicos que tengan conversaciones, " +
                     "ordenados por el mensaje más reciente (más reciente primero). " +
                     "Permite búsqueda en nombre del contacto, teléfono y contenido de mensajes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de contactos obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "contacts": [
                        {
                          "contactId": "987fbc97-4bed-5078-9f07-9141ba07c9f3",
                          "contact": {
                            "id": "987fbc97-4bed-5078-9f07-9141ba07c9f3",
                            "displayName": "Juan Pérez",
                            "phone": "+593991234567",
                            "email": "juan@example.com"
                          },
                          "lastMessage": {
                            "id": "msg-001",
                            "content": "Gracias por su respuesta",
                            "direction": "INBOUND",
                            "createdAt": "2025-10-03T11:00:00Z"
                          },
                          "lastConversation": {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "status": "OPEN",
                            "channel": "WHATSAPP"
                          }
                        }
                      ],
                      "total": 150,
                      "page": 0,
                      "size": 20,
                      "totalPages": 8
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetros inválidos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "ID de cliente inválido"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/contacts")
    public ResponseEntity<Map<String, Object>> getContactsWithConversations(
        @Parameter(description = "UUID del cliente", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
        @RequestParam String clientId,
        
        @Parameter(description = "Texto de búsqueda (busca en nombre, teléfono y mensajes)")
        @RequestParam(required = false) String query,
        
        @Parameter(description = "Número de página (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Tamaño de página", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            UuidId<Client> clientUuidId = UuidId.of(UUID.fromString(clientId));
            
            ConversationRepository.ContactConversationResult result = getContactsWithConversations.handle(
                clientUuidId,
                query,
                page,
                size
            );
            
            // Convertir a DTOs
            List<Map<String, Object>> contactDtos = result.contacts().stream()
                .map(info -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("contactId", info.contact().id().value().toString());
                    
                    // Información del contacto
                    Map<String, Object> contactDto = new HashMap<>();
                    contactDto.put("id", info.contact().id().value().toString());
                    contactDto.put("displayName", info.contact().displayName() != null ? info.contact().displayName() : "");
                    contactDto.put("phone", info.contact().phoneE164() != null ? info.contact().phoneE164().value() : null);
                    contactDto.put("email", info.contact().email() != null ? info.contact().email().value() : null);
                    dto.put("contact", contactDto);
                    
                    // Último mensaje
                    if (info.lastMessage() != null) {
                        Map<String, Object> lastMessageDto = new HashMap<>();
                        lastMessageDto.put("id", info.lastMessage().id().value().toString());
                        lastMessageDto.put("content", info.lastMessage().content() != null ? info.lastMessage().content() : "");
                        lastMessageDto.put("direction", info.lastMessage().direction().name());
                        lastMessageDto.put("createdAt", info.lastMessage().createdAt());
                        dto.put("lastMessage", lastMessageDto);
                    } else {
                        dto.put("lastMessage", null);
                    }
                    
                    // Última conversación
                    if (info.lastConversation() != null) {
                        Map<String, Object> lastConvDto = new HashMap<>();
                        lastConvDto.put("id", info.lastConversation().id().value().toString());
                        lastConvDto.put("status", info.lastConversation().status().name());
                        lastConvDto.put("channel", info.lastConversation().channel().name());
                        dto.put("lastConversation", lastConvDto);
                    } else {
                        dto.put("lastConversation", null);
                    }
                    
                    return dto;
                })
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "contacts", contactDtos,
                "total", result.total(),
                "page", result.page(),
                "size", result.size(),
                "totalPages", result.totalPages()
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Parámetro inválido: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener contactos con conversaciones: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener contactos con conversaciones: " + e.getMessage()
            ));
        }
    }
}
