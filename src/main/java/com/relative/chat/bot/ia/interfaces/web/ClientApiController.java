package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.services.WhatsAppTemplateService;
import com.relative.chat.bot.ia.application.usecases.CloseConversation;
import com.relative.chat.bot.ia.application.usecases.GetConversationHistory;
import com.relative.chat.bot.ia.application.usecases.ListConversations;
import com.relative.chat.bot.ia.application.usecases.SendBulkTemplate;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.*;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.vo.Email;
import com.relative.chat.bot.ia.domain.vo.PhoneE164;
import com.relative.chat.bot.ia.infrastructure.security.SecurityUtils;
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
import java.util.stream.Collectors;

/**
 * Controller unificado para API de clientes externos
 * Todos los endpoints requieren autenticación mediante token
 * Base path: /api/v1
 * 
 * IMPORTANTE: Este controller es nuevo y NO modifica los endpoints existentes
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Client API", description = "API unificada para clientes externos. Requiere autenticación mediante token.")
public class ClientApiController {
    
    // Repositorios y servicios
    private final ContactRepository contactRepository;
    private final ConversationRepository conversationRepository;
    private final WhatsAppTemplateService templateService;
    private final ClientPhoneRepository clientPhoneRepository;
    private final GetConversationHistory getConversationHistory;
    private final CloseConversation closeConversation;
    private final ListConversations listConversations;
    private final SendBulkTemplate sendBulkTemplate;
    
    // ==================== CONTACTOS ====================
    
    @Operation(
        summary = "Listar contactos del cliente autenticado",
        description = "Obtiene todos los contactos del cliente autenticado. El clientId se obtiene automáticamente del token de autenticación."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contactos obtenidos exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "contacts": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "displayName": "Juan Pérez",
                          "fullName": "Juan Pérez",
                          "email": "juan.perez@email.com",
                          "phone": "+525512345678",
                          "isVip": true,
                          "isActive": true,
                          "totalInteractions": 15,
                          "lastSeenAt": "2024-12-19T10:30:00Z",
                          "createdAt": "2024-12-01T08:00:00Z"
                        }
                      ],
                      "total": 1
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Token requerido. Use header 'Authorization: Bearer <token>' o 'X-API-Key: <token>'"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/contacts")
    public ResponseEntity<Map<String, Object>> getContacts() {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            List<Contact> contacts = contactRepository.findByClientId(clientId);
            
            List<Map<String, Object>> contactDtos = contacts.stream()
                .map(this::toBasicContactDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "contacts", contactDtos,
                "total", contactDtos.size()
            ));
        } catch (Exception e) {
            log.error("Error al obtener contactos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener contactos: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Obtener contacto por ID",
        description = "Obtiene un contacto específico del cliente autenticado. Solo puede acceder a contactos que pertenecen a su cuenta."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contacto obtenido exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "contact": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "clientId": "123e4567-e89b-12d3-a456-426614174000",
                        "displayName": "Juan Pérez",
                        "firstName": "Juan",
                        "lastName": "Pérez",
                        "email": "juan.perez@email.com",
                        "phone": "+525512345678",
                        "isVip": true,
                        "isActive": true,
                        "tags": ["VIP", "PREMIUM"],
                        "createdAt": "2024-12-01T08:00:00Z",
                        "updatedAt": "2024-12-19T10:30:00Z"
                      }
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
        @ApiResponse(
            responseCode = "403",
            description = "Contacto no pertenece al cliente autenticado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "No tiene acceso a este contacto"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/contacts/{id}")
    public ResponseEntity<Map<String, Object>> getContactById(
        @Parameter(description = "UUID del contacto", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            Optional<Contact> contactOpt = contactRepository.findById(UuidId.of(UUID.fromString(id)));
            
            if (contactOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Contacto no encontrado"
                ));
            }
            
            Contact contact = contactOpt.get();
            
            // Validar que el contacto pertenece al cliente autenticado
            if (!contact.clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "No tiene acceso a este contacto"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "contact", toDetailedContactDto(contact)
            ));
        } catch (Exception e) {
            log.error("Error al obtener contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener contacto: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Crear contacto",
        description = "Crea un nuevo contacto para el cliente autenticado. El clientId se asigna automáticamente desde el token."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Contacto creado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "contactId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "Contacto creado exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "displayName, firstName, lastName, phone y email son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(value = "/contacts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createContact(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del contacto a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "displayName": "Juan Pérez",
                      "firstName": "Juan",
                      "lastName": "Pérez",
                      "phone": "+525512345678",
                      "email": "juan.perez@email.com",
                      "isVip": false,
                      "tags": ["CLIENTE"]
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            String displayName = (String) request.get("displayName");
            String firstName = (String) request.get("firstName");
            String lastName = (String) request.get("lastName");
            String phone = (String) request.get("phone");
            String email = (String) request.get("email");
            
            if (displayName == null || firstName == null || lastName == null || phone == null || email == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "displayName, firstName, lastName, phone y email son requeridos"
                ));
            }
            
            Contact contact = Contact.create(
                clientId,
                displayName,
                firstName,
                lastName,
                new PhoneE164(phone),
                new Email(email)
            );
            
            contact = updateContactWithAdditionalData(contact, request);
            contactRepository.save(contact);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "contactId", contact.id().value().toString(),
                "message", "Contacto creado exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error al crear contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al crear contacto: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Actualizar contacto",
        description = "Actualiza la información de un contacto existente. Solo puede actualizar contactos que pertenecen a su cuenta."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contacto actualizado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "message": "Contacto actualizado exitosamente"
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
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tiene acceso a este contacto"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping(value = "/contacts/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateContact(
        @Parameter(description = "UUID del contacto a actualizar", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del contacto a actualizar",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "displayName": "Juan Carlos Pérez",
                      "isVip": true,
                      "tags": ["VIP", "PREMIUM"]
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            Optional<Contact> contactOpt = contactRepository.findById(UuidId.of(UUID.fromString(id)));
            if (contactOpt.isEmpty() || !contactOpt.get().clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Contacto no encontrado"
                ));
            }
            
            Contact updated = updateContactWithAdditionalData(contactOpt.get(), request);
            contactRepository.save(updated);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Contacto actualizado exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error al actualizar contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al actualizar contacto: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Eliminar contacto",
        description = "Elimina un contacto. Solo puede eliminar contactos que pertenecen a su cuenta."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contacto eliminado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "message": "Contacto eliminado exitosamente"
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
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tiene acceso a este contacto"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/contacts/{id}")
    public ResponseEntity<Map<String, Object>> deleteContact(
        @Parameter(description = "UUID del contacto a eliminar", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            Optional<Contact> contactOpt = contactRepository.findById(UuidId.of(UUID.fromString(id)));
            if (contactOpt.isEmpty() || !contactOpt.get().clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Contacto no encontrado"
                ));
            }
            
            contactRepository.delete(contactOpt.get().id());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Contacto eliminado exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error al eliminar contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al eliminar contacto: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Buscar contactos",
        description = "Busca contactos del cliente autenticado con filtros opcionales. Soporta paginación."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Búsqueda exitosa",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "contacts": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "displayName": "Juan Pérez",
                          "email": "juan.perez@email.com",
                          "phone": "+525512345678",
                          "isVip": true
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
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/contacts/search")
    public ResponseEntity<Map<String, Object>> searchContacts(
        @Parameter(description = "Texto de búsqueda (nombre, email, teléfono)")
        @RequestParam(required = false) String query,
        @Parameter(description = "Filtrar por VIP")
        @RequestParam(required = false) Boolean isVip,
        @Parameter(description = "Filtrar por activo")
        @RequestParam(required = false) Boolean isActive,
        @Parameter(description = "Etiqueta para filtrar")
        @RequestParam(required = false) String tag,
        @Parameter(description = "Número de página (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Tamaño de página", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            ContactRepository.SearchResult result = contactRepository.searchContacts(
                clientId, query, isVip, isActive, tag, page, size
            );
            
            List<Map<String, Object>> dtos = result.contacts().stream()
                .map(this::toBasicContactDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "contacts", dtos,
                "total", result.total(),
                "page", result.page(),
                "size", result.size(),
                "totalPages", result.totalPages()
            ));
        } catch (Exception e) {
            log.error("Error al buscar contactos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al buscar contactos: " + e.getMessage()
            ));
        }
    }
    
    // ==================== PLANTILLAS ====================
    
    @Operation(
        summary = "Listar plantillas del cliente autenticado",
        description = "Obtiene todas las plantillas de WhatsApp asociadas a los teléfonos del cliente autenticado."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plantillas obtenidas exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "templates": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "name": "welcome_message",
                          "category": "MARKETING",
                          "status": "APPROVED",
                          "language": "es_ES",
                          "canBeSent": true,
                          "createdAt": "2024-12-01T08:00:00Z"
                        }
                      ],
                      "total": 1
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getTemplates() {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            // Obtener todos los teléfonos del cliente
            List<com.relative.chat.bot.ia.domain.messaging.ClientPhone> phones = clientPhoneRepository.findByClient(clientId);
            
            List<WhatsAppTemplate> allTemplates = new ArrayList<>();
            for (com.relative.chat.bot.ia.domain.messaging.ClientPhone phone : phones) {
                List<WhatsAppTemplate> templates = templateService.getTemplatesByClient(phone.id());
                allTemplates.addAll(templates);
            }
            
            List<Map<String, Object>> templateDtos = allTemplates.stream()
                .map(this::toTemplateDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "templates", templateDtos,
                "total", templateDtos.size()
            ));
        } catch (Exception e) {
            log.error("Error al obtener plantillas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener plantillas: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Obtener plantilla por ID",
        description = "Obtiene los detalles de una plantilla específica. Solo puede acceder a plantillas de sus teléfonos."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plantilla obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "template": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "name": "welcome_message",
                        "category": "MARKETING",
                        "status": "APPROVED",
                        "language": "es_ES",
                        "parameterFormat": "POSITIONAL",
                        "canBeSent": true,
                        "components": [
                          {
                            "type": "BODY",
                            "text": "Bienvenido {{1}}"
                          }
                        ],
                        "metaTemplateId": "123456789012345",
                        "createdAt": "2024-12-01T08:00:00Z",
                        "updatedAt": "2024-12-01T08:00:00Z"
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Plantilla no encontrada",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Error al obtener plantilla: Plantilla no encontrada"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene acceso a esta plantilla",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "No tiene acceso a esta plantilla"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/templates/{id}")
    public ResponseEntity<Map<String, Object>> getTemplateById(
        @Parameter(description = "UUID de la plantilla", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            WhatsAppTemplate template = templateService.findById(UuidId.of(UUID.fromString(id)));
            
            // Validar que la plantilla pertenece al cliente
            com.relative.chat.bot.ia.domain.messaging.ClientPhone phone = clientPhoneRepository
                .findById(template.clientPhoneId())
                .orElseThrow(() -> new IllegalArgumentException("Teléfono no encontrado"));
            
            if (!phone.clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "No tiene acceso a esta plantilla"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "template", toTemplateDtoWithComponents(template)
            ));
        } catch (Exception e) {
            log.error("Error al obtener plantilla: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener plantilla: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Obtener plantillas listas para usar",
        description = "Obtiene todas las plantillas que están aprobadas y listas para ser enviadas (canBeSent = true)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plantillas listas obtenidas exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "templates": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "name": "welcome_message",
                          "category": "MARKETING",
                          "status": "APPROVED",
                          "language": "es_ES",
                          "canBeSent": true
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/templates/ready")
    public ResponseEntity<Map<String, Object>> getReadyTemplates() {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            List<com.relative.chat.bot.ia.domain.messaging.ClientPhone> phones = clientPhoneRepository.findByClient(clientId);
            
            List<WhatsAppTemplate> readyTemplates = new ArrayList<>();
            for (com.relative.chat.bot.ia.domain.messaging.ClientPhone phone : phones) {
                List<WhatsAppTemplate> templates = templateService.getTemplatesByClient(phone.id());
                readyTemplates.addAll(templates.stream()
                    .filter(WhatsAppTemplate::canBeSent)
                    .collect(Collectors.toList()));
            }
            
            List<Map<String, Object>> templateDtos = readyTemplates.stream()
                .map(this::toTemplateDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "templates", templateDtos
            ));
        } catch (Exception e) {
            log.error("Error al obtener plantillas listas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener plantillas: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Crear plantilla de WhatsApp",
        description = "Crea una nueva plantilla de WhatsApp para un teléfono del cliente autenticado. Reutiliza la implementación de WhatsAppTemplateController."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Plantilla creada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "templateId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "Plantilla creada exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "clientPhoneId, name, category y components son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene acceso a este teléfono",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "No tiene acceso a este teléfono"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(value = "/templates", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createTemplate(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos de la plantilla a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientPhoneId": "a1234567-e89b-12d3-a456-426614174000",
                      "name": "otp_verification",
                      "category": "AUTHENTICATION",
                      "language": "es_ES",
                      "parameterFormat": "POSITIONAL",
                      "components": [
                        {
                          "type": "BODY",
                          "text": "Tu código de verificación es: {{1}}. Este código expira en {{2}} minutos."
                        }
                      ]
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            String clientPhoneIdStr = (String) request.get("clientPhoneId");
            if (clientPhoneIdStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientPhoneId es requerido"
                ));
            }
            
            UuidId<com.relative.chat.bot.ia.domain.messaging.ClientPhone> clientPhoneId = UuidId.of(UUID.fromString(clientPhoneIdStr));
            
            // Validar que el teléfono pertenece al cliente autenticado
            com.relative.chat.bot.ia.domain.messaging.ClientPhone phone = clientPhoneRepository.findById(clientPhoneId)
                .orElseThrow(() -> new IllegalArgumentException("Teléfono no encontrado"));
            
            if (!phone.clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "No tiene acceso a este teléfono"
                ));
            }
            
            // Reutilizar la lógica de WhatsAppTemplateController
            String name = (String) request.get("name");
            String category = (String) request.get("category");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> componentsData = (List<Map<String, Object>>) request.get("components");
            
            if (name == null || category == null || componentsData == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "name, category y components son requeridos"
                ));
            }
            
            String language = (String) request.getOrDefault("language", "es_ES");
            String parameterFormat = (String) request.getOrDefault("parameterFormat", "POSITIONAL");
            
            List<TemplateComponent> components = parseTemplateComponents(componentsData);
            
            WhatsAppTemplate template = templateService.createTemplate(
                clientPhoneId,
                name,
                TemplateCategory.valueOf(category),
                language,
                ParameterFormat.valueOf(parameterFormat),
                components
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "templateId", template.id().value().toString(),
                "message", "Plantilla creada exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al crear plantilla: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al crear plantilla: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Listar plantillas por estado",
        description = "Obtiene todas las plantillas del cliente autenticado filtradas por estado. Reutiliza la implementación de WhatsAppTemplateController."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plantillas obtenidas exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "templates": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "name": "welcome_message",
                          "category": "MARKETING",
                          "status": "APPROVED",
                          "language": "es_ES"
                        }
                      ],
                      "count": 1
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Estado inválido",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Estado inválido: INVALID_STATUS"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(value = {"/templates/status/{status}", "/templates/status"})
    public ResponseEntity<Map<String, Object>> getTemplatesByStatus(
        @Parameter(description = "Estado de la plantilla (opcional). Valores: APPROVED, PENDING, REJECTED, DISABLED, DRAFT, o 'all' para obtener todas", example = "APPROVED")
        @PathVariable(required = false) String status
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            List<com.relative.chat.bot.ia.domain.messaging.ClientPhone> phones = clientPhoneRepository.findByClient(clientId);
            
            List<WhatsAppTemplate> allTemplates = new ArrayList<>();
            for (com.relative.chat.bot.ia.domain.messaging.ClientPhone phone : phones) {
                List<WhatsAppTemplate> templates = templateService.getTemplatesByClient(phone.id());
                allTemplates.addAll(templates);
            }
            
            // Filtrar por estado si se proporciona
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
                TemplateStatus templateStatus = TemplateStatus.valueOf(status.toUpperCase());
                allTemplates = allTemplates.stream()
                    .filter(t -> t.status() == templateStatus)
                    .collect(Collectors.toList());
            }
            
            List<Map<String, Object>> templateDtos = allTemplates.stream()
                .map(this::toTemplateDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "templates", templateDtos,
                "count", templateDtos.size()
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Estado inválido: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Estado inválido: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener plantillas por estado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener plantillas: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Listar plantillas por categoría",
        description = "Obtiene todas las plantillas del cliente autenticado filtradas por categoría. Reutiliza la implementación de WhatsAppTemplateController."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plantillas obtenidas exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "templates": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "name": "otp_verification",
                          "category": "AUTHENTICATION",
                          "status": "APPROVED"
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Categoría inválida",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Error al obtener plantillas por categoría: Categoría inválida"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/templates/category/{category}")
    public ResponseEntity<Map<String, Object>> getTemplatesByCategory(
        @Parameter(description = "Categoría de la plantilla. Valores: AUTHENTICATION, MARKETING, UTILITY", required = true, example = "AUTHENTICATION")
        @PathVariable String category
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            TemplateCategory templateCategory = TemplateCategory.valueOf(category);
            
            List<com.relative.chat.bot.ia.domain.messaging.ClientPhone> phones = clientPhoneRepository.findByClient(clientId);
            
            List<WhatsAppTemplate> allTemplates = new ArrayList<>();
            for (com.relative.chat.bot.ia.domain.messaging.ClientPhone phone : phones) {
                List<WhatsAppTemplate> templates = templateService.getTemplatesByClient(phone.id());
                allTemplates.addAll(templates.stream()
                    .filter(t -> t.category() == templateCategory)
                    .collect(Collectors.toList()));
            }
            
            List<Map<String, Object>> templateDtos = allTemplates.stream()
                .map(this::toTemplateDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "templates", templateDtos
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener plantillas por categoría: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener plantillas: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Sincronizar plantilla con Meta",
        description = "Envía una plantilla a Meta WhatsApp Business API para su revisión y aprobación. Reutiliza la implementación de WhatsAppTemplateController."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plantilla sincronizada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "templateId": "550e8400-e29b-41d4-a716-446655440000",
                      "metaTemplateId": "123456789012345",
                      "message": "Plantilla sincronizada con Meta exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Plantilla no está en estado DRAFT",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Solo se pueden sincronizar plantillas en estado DRAFT"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene acceso a esta plantilla",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "No tiene acceso a esta plantilla"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/templates/{id}/sync")
    public ResponseEntity<Map<String, Object>> syncTemplateToMeta(
        @Parameter(description = "UUID de la plantilla a sincronizar", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            WhatsAppTemplate template = templateService.findById(UuidId.of(UUID.fromString(id)));
            
            // Validar que la plantilla pertenece al cliente
            com.relative.chat.bot.ia.domain.messaging.ClientPhone phone = clientPhoneRepository
                .findById(template.clientPhoneId())
                .orElseThrow(() -> new IllegalArgumentException("Teléfono no encontrado"));
            
            if (!phone.clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "No tiene acceso a esta plantilla"
                ));
            }
            
            // Reutilizar la implementación de WhatsAppTemplateController
            WhatsAppTemplate syncedTemplate = templateService.syncTemplateToMeta(template.id());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "templateId", syncedTemplate.id().value().toString(),
                "metaTemplateId", syncedTemplate.metaTemplateId(),
                "message", "Plantilla sincronizada con Meta exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al sincronizar plantilla: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al sincronizar con Meta: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Actualizar estado de plantilla desde Meta",
        description = "Actualiza el estado de una plantilla consultando directamente a Meta API. Reutiliza la implementación de WhatsAppTemplateController."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Estado actualizado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "templateId": "550e8400-e29b-41d4-a716-446655440000",
                      "newStatus": "APPROVED",
                      "qualityRating": "HIGH",
                      "message": "Estado actualizado desde Meta exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Plantilla no está sincronizada con Meta",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "La plantilla no está sincronizada con Meta"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene acceso a esta plantilla",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "No tiene acceso a esta plantilla"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/templates/{id}/update-status")
    public ResponseEntity<Map<String, Object>> updateTemplateStatus(
        @Parameter(description = "UUID de la plantilla", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            WhatsAppTemplate template = templateService.findById(UuidId.of(UUID.fromString(id)));
            
            // Validar que la plantilla pertenece al cliente
            com.relative.chat.bot.ia.domain.messaging.ClientPhone phone = clientPhoneRepository
                .findById(template.clientPhoneId())
                .orElseThrow(() -> new IllegalArgumentException("Teléfono no encontrado"));
            
            if (!phone.clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "No tiene acceso a esta plantilla"
                ));
            }
            
            // Reutilizar la implementación de WhatsAppTemplateController
            WhatsAppTemplate updatedTemplate = templateService.updateTemplateStatusFromMeta(template.id());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "templateId", updatedTemplate.id().value().toString(),
                "newStatus", updatedTemplate.status().name(),
                "qualityRating", updatedTemplate.qualityRating().name(),
                "message", "Estado actualizado desde Meta exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al actualizar estado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al actualizar estado: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Actualizar plantilla de WhatsApp",
        description = "Actualiza una plantilla existente. Solo se pueden actualizar plantillas en estado DRAFT. Los campos opcionales que no se envíen mantendrán sus valores actuales. Reutiliza la implementación de WhatsAppTemplateController."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plantilla actualizada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "templateId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "Plantilla actualizada exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o plantilla no está en estado DRAFT",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Solo se pueden actualizar plantillas en estado DRAFT. Estado actual: APPROVED"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene acceso a esta plantilla",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "No tiene acceso a esta plantilla"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Plantilla no encontrada",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Plantilla no encontrada: 550e8400-e29b-41d4-a716-446655440000"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PutMapping(value = "/templates/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateTemplate(
        @Parameter(description = "UUID de la plantilla a actualizar", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos de la plantilla a actualizar. Todos los campos son opcionales.",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "name": "otp_verification_updated",
                      "language": "es_ES",
                      "parameterFormat": "POSITIONAL",
                      "components": [
                        {
                          "type": "BODY",
                          "text": "Tu código de verificación es: {{1}}. Este código expira en {{2}} minutos."
                        }
                      ]
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            UuidId<WhatsAppTemplate> templateId = UuidId.of(UUID.fromString(id));
            
            WhatsAppTemplate template = templateService.findById(templateId);
            
            // Validar que la plantilla pertenece al cliente
            com.relative.chat.bot.ia.domain.messaging.ClientPhone phone = clientPhoneRepository
                .findById(template.clientPhoneId())
                .orElseThrow(() -> new IllegalArgumentException("Teléfono no encontrado"));
            
            if (!phone.clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "No tiene acceso a esta plantilla"
                ));
            }
            
            // Obtener valores opcionales del request
            String name = (String) request.get("name");
            String language = (String) request.get("language");
            String parameterFormatStr = (String) request.get("parameterFormat");
            ParameterFormat parameterFormat = null;
            if (parameterFormatStr != null) {
                parameterFormat = ParameterFormat.valueOf(parameterFormatStr);
            }
            
            // Parsear componentes si se proporcionan
            List<TemplateComponent> components = null;
            if (request.containsKey("components")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> componentsData = (List<Map<String, Object>>) request.get("components");
                components = parseTemplateComponents(componentsData);
            }
            
            // Actualizar plantilla usando el servicio
            WhatsAppTemplate updatedTemplate = templateService.updateTemplate(
                templateId,
                name,
                language,
                parameterFormat,
                components
            );
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "templateId", updatedTemplate.id().value().toString(),
                "message", "Plantilla actualizada exitosamente"
            ));
            
        } catch (IllegalStateException e) {
            log.error("Error de estado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al actualizar plantilla: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al actualizar plantilla: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Eliminar plantilla",
        description = "Elimina una plantilla tanto de la base de datos local como de Meta API. Reutiliza la implementación de WhatsAppTemplateController."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plantilla eliminada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "message": "Plantilla eliminada exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Plantilla no encontrada",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Plantilla no encontrada"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene acceso a esta plantilla",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "No tiene acceso a esta plantilla"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(
        @Parameter(description = "UUID de la plantilla a eliminar", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            WhatsAppTemplate template = templateService.findById(UuidId.of(UUID.fromString(id)));
            
            // Validar que la plantilla pertenece al cliente
            com.relative.chat.bot.ia.domain.messaging.ClientPhone phone = clientPhoneRepository
                .findById(template.clientPhoneId())
                .orElseThrow(() -> new IllegalArgumentException("Teléfono no encontrado"));
            
            if (!phone.clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "No tiene acceso a esta plantilla"
                ));
            }
            
            // Reutilizar la implementación de WhatsAppTemplateController
            templateService.deleteTemplate(template.id());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Plantilla eliminada exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al eliminar plantilla: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al eliminar plantilla: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Sincronizar todas las plantillas pendientes",
        description = "Sincroniza todas las plantillas del cliente autenticado que están en estado DRAFT con Meta WhatsApp Business API. Reutiliza la implementación de WhatsAppTemplateController."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sincronización iniciada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "message": "Sincronización de plantillas pendientes iniciada"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/templates/sync-all")
    public ResponseEntity<Map<String, Object>> syncAllPendingTemplates() {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            List<com.relative.chat.bot.ia.domain.messaging.ClientPhone> phones = clientPhoneRepository.findByClient(clientId);
            
            // Sincronizar plantillas DRAFT de todos los teléfonos del cliente
            for (com.relative.chat.bot.ia.domain.messaging.ClientPhone phone : phones) {
                List<WhatsAppTemplate> templates = templateService.getTemplatesByClient(phone.id());
                for (WhatsAppTemplate template : templates) {
                    if (template.status() == TemplateStatus.DRAFT) {
                        try {
                            templateService.syncTemplateToMeta(template.id());
                        } catch (Exception e) {
                            log.warn("Error al sincronizar plantilla {}: {}", template.id().value(), e.getMessage());
                        }
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Sincronización de plantillas pendientes iniciada"
            ));
            
        } catch (Exception e) {
            log.error("Error al sincronizar plantillas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al sincronizar plantillas: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Actualizar estados de todas las plantillas sincronizadas",
        description = "Consulta Meta WhatsApp Business API para actualizar el estado de todas las plantillas del cliente autenticado que están sincronizadas con Meta. Reutiliza la implementación de WhatsAppTemplateController."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Actualización de estados iniciada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "message": "Actualización de estados de plantillas iniciada"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/templates/update-all-statuses")
    public ResponseEntity<Map<String, Object>> updateAllTemplateStatuses() {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            List<com.relative.chat.bot.ia.domain.messaging.ClientPhone> phones = clientPhoneRepository.findByClient(clientId);
            
            // Actualizar estados de todas las plantillas sincronizadas del cliente
            for (com.relative.chat.bot.ia.domain.messaging.ClientPhone phone : phones) {
                List<WhatsAppTemplate> templates = templateService.getTemplatesByClient(phone.id());
                for (WhatsAppTemplate template : templates) {
                    if (template.isSyncedWithMeta()) {
                        try {
                            templateService.updateTemplateStatusFromMeta(template.id());
                        } catch (Exception e) {
                            log.warn("Error al actualizar estado de plantilla {}: {}", template.id().value(), e.getMessage());
                        }
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Actualización de estados de plantillas iniciada"
            ));
            
        } catch (Exception e) {
            log.error("Error al actualizar estados: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al actualizar estados: " + e.getMessage()
            ));
        }
    }
    
    // ==================== ENVÍO MASIVO ====================
    
    @Operation(
        summary = "Envío masivo con filtros",
        description = "Envía plantillas a múltiples contactos del cliente autenticado usando filtros específicos. Solo puede usar teléfonos de su cuenta."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Envío masivo completado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "result": {
                        "totalContacts": 150,
                        "successfulSends": 145,
                        "failedSends": 5,
                        "errors": [
                          "Error en contacto xyz: Template no aprobado"
                        ],
                        "startedAt": "2024-12-19T10:30:00Z",
                        "completedAt": "2024-12-19T10:35:00Z"
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "phoneId y templateName son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene acceso a este teléfono",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "No tiene acceso a este teléfono"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(value = "/templates/send/bulk", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendBulkTemplate(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos para envío masivo con filtros",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "phoneId": "b1234567-e89b-12d3-a456-426614174000",
                      "templateName": "marketing_promotion",
                      "parameterFormat": "NAMED",
                      "parameters": {
                        "product_name": "Nuevo Producto",
                        "discount": "20%"
                      },
                      "filters": {
                        "onlyActive": true,
                        "onlyVip": false,
                        "tagNames": ["VIP", "PREMIUM"],
                        "preferredContactMethod": "WHATSAPP",
                        "marketingConsent": true
                      }
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            String phoneId = (String) request.get("phoneId");
            String templateName = (String) request.get("templateName");
            
            if (phoneId == null || templateName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "phoneId y templateName son requeridos"
                ));
            }
            
            // Validar que el teléfono pertenece al cliente
            com.relative.chat.bot.ia.domain.messaging.ClientPhone phone = clientPhoneRepository
                .findById(UuidId.of(UUID.fromString(phoneId)))
                .orElseThrow(() -> new IllegalArgumentException("Teléfono no encontrado"));
            
            if (!phone.clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "No tiene acceso a este teléfono"
                ));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) request.getOrDefault("parameters", new HashMap<>());
            
            String parameterFormatStr = (String) request.getOrDefault("parameterFormat", "NAMED");
            ParameterFormat parameterFormat = ParameterFormat.valueOf(parameterFormatStr.toUpperCase());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> filtersMap = (Map<String, Object>) request.getOrDefault("filters", new HashMap<>());
            SendBulkTemplate.BulkSendFilters filters = parseBulkFilters(filtersMap);
            
            SendBulkTemplate.BulkSendResult result = sendBulkTemplate.handle(
                clientId,
                UuidId.of(UUID.fromString(phoneId)),
                templateName,
                parameters,
                parameterFormat,
                filters
            );
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "result", Map.of(
                    "totalContacts", result.totalContacts(),
                    "successfulSends", result.successfulSends(),
                    "failedSends", result.failedSends(),
                    "errors", result.errors(),
                    "startedAt", result.startedAt(),
                    "completedAt", result.completedAt()
                )
            ));
        } catch (Exception e) {
            log.error("Error en envío masivo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error en envío masivo: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Envío masivo a contactos VIP",
        description = "Envía plantillas específicamente a contactos marcados como VIP del cliente autenticado."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Envío masivo VIP completado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "result": {
                        "totalContacts": 25,
                        "successfulSends": 24,
                        "failedSends": 1,
                        "errors": [],
                        "startedAt": "2024-12-19T10:30:00Z",
                        "completedAt": "2024-12-19T10:32:00Z"
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "phoneId y templateName son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(value = "/templates/send/bulk/vip", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendBulkToVip(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos para envío masivo a contactos VIP",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "phoneId": "b1234567-e89b-12d3-a456-426614174000",
                      "templateName": "vip_promotion",
                      "parameters": {
                        "discount": "30%",
                        "product_name": "Producto Premium"
                      }
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            String phoneId = (String) request.get("phoneId");
            String templateName = (String) request.get("templateName");
            
            if (phoneId == null || templateName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "phoneId y templateName son requeridos"
                ));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) request.getOrDefault("parameters", new HashMap<>());
            
            SendBulkTemplate.BulkSendResult result = sendBulkTemplate.handle(
                clientId,
                UuidId.of(UUID.fromString(phoneId)),
                templateName,
                parameters,
                ParameterFormat.NAMED,
                SendBulkTemplate.BulkSendFilters.forVipContacts()
            );
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "result", Map.of(
                    "totalContacts", result.totalContacts(),
                    "successfulSends", result.successfulSends(),
                    "failedSends", result.failedSends(),
                    "errors", result.errors(),
                    "startedAt", result.startedAt(),
                    "completedAt", result.completedAt()
                )
            ));
        } catch (Exception e) {
            log.error("Error en envío masivo VIP: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error en envío masivo: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Envío masivo por tags",
        description = "Envía plantillas a contactos del cliente autenticado que tienen uno o más tags específicos."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Envío masivo por tags completado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "result": {
                        "totalContacts": 50,
                        "successfulSends": 48,
                        "failedSends": 2,
                        "errors": [],
                        "startedAt": "2024-12-19T10:30:00Z",
                        "completedAt": "2024-12-19T10:33:00Z"
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "phoneId, templateName y tagNames son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(value = "/templates/send/bulk/by-tags", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendBulkByTags(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos para envío masivo por tags",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "phoneId": "b1234567-e89b-12d3-a456-426614174000",
                      "templateName": "tagged_promotion",
                      "tagNames": ["VIP", "PREMIUM", "TECH"],
                      "parameters": {
                        "product_name": "Nuevo Producto"
                      }
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            String phoneId = (String) request.get("phoneId");
            String templateName = (String) request.get("templateName");
            
            @SuppressWarnings("unchecked")
            List<String> tagNames = (List<String>) request.get("tagNames");
            
            if (phoneId == null || templateName == null || tagNames == null || tagNames.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "phoneId, templateName y tagNames son requeridos"
                ));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) request.getOrDefault("parameters", new HashMap<>());
            
            SendBulkTemplate.BulkSendResult result = sendBulkTemplate.handle(
                clientId,
                UuidId.of(UUID.fromString(phoneId)),
                templateName,
                parameters,
                ParameterFormat.NAMED,
                SendBulkTemplate.BulkSendFilters.forTaggedContacts(tagNames)
            );
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "result", Map.of(
                    "totalContacts", result.totalContacts(),
                    "successfulSends", result.successfulSends(),
                    "failedSends", result.failedSends(),
                    "errors", result.errors(),
                    "startedAt", result.startedAt(),
                    "completedAt", result.completedAt()
                )
            ));
        } catch (Exception e) {
            log.error("Error en envío masivo por tags: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error en envío masivo: " + e.getMessage()
            ));
        }
    }
    
    // ==================== CONVERSACIONES ====================
    
    @Operation(
        summary = "Obtener conversación con historial",
        description = "Obtiene una conversación específica con su historial completo de mensajes. Solo puede acceder a conversaciones de su cuenta."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversación obtenida exitosamente",
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
                        },
                        {
                          "id": "msg-002",
                          "direction": "OUTBOUND",
                          "content": "Hola, ¿en qué puedo ayudarte?",
                          "createdAt": "2025-10-03T10:30:15Z"
                        }
                      ]
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
        @ApiResponse(
            responseCode = "403",
            description = "No tiene acceso a esta conversación",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "No tiene acceso a esta conversación"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/conversations/{id}")
    public ResponseEntity<Map<String, Object>> getConversation(
        @Parameter(description = "UUID de la conversación", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            Optional<Conversation> conversationOpt = conversationRepository.findById(UuidId.of(UUID.fromString(id)));
            
            if (conversationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Conversación no encontrada"
                ));
            }
            
            Conversation conv = conversationOpt.get();
            
            // Validar que la conversación pertenece al cliente
            if (!conv.clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "error",
                    "message", "No tiene acceso a esta conversación"
                ));
            }
            
            List<Message> messages = getConversationHistory.handle(conv.id(), 100);
            
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener conversación: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Cerrar conversación",
        description = "Marca una conversación como cerrada. Una vez cerrada, no se podrán enviar más mensajes. Solo puede cerrar conversaciones de su cuenta."
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
        @ApiResponse(
            responseCode = "400",
            description = "Error al cerrar conversación",
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
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/conversations/{id}/close")
    public ResponseEntity<Map<String, Object>> closeConversation(
        @Parameter(description = "UUID de la conversación", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable String id
    ) {
        try {
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId = SecurityUtils.requireAuthenticatedClientId();
            
            Optional<Conversation> conversationOpt = conversationRepository.findById(UuidId.of(UUID.fromString(id)));
            
            if (conversationOpt.isEmpty() || !conversationOpt.get().clientId().equals(clientId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Conversación no encontrada"
                ));
            }
            
            closeConversation.handle(conversationOpt.get().id());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Conversación cerrada exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error al cerrar conversación: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al cerrar conversación: " + e.getMessage()
            ));
        }
    }
    
    @Operation(
        summary = "Listar conversaciones del cliente autenticado",
        description = "Lista todas las conversaciones del cliente autenticado con capacidad de búsqueda en títulos, mensajes y contactos. " +
                     "Funciona como una lista de chats donde cada conversación muestra información del contacto y último mensaje. " +
                     "El clientId se obtiene automáticamente del token de autenticación."
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
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> listConversations(
        @Parameter(description = "Texto de búsqueda (busca en títulos, mensajes y contactos)")
        @RequestParam(required = false) String query,
        
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
            // Obtener clientId del token de autenticación
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientUuidId = SecurityUtils.requireAuthenticatedClientId();
            
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
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    @SuppressWarnings("unchecked")
    private Contact updateContactWithAdditionalData(Contact contact, Map<String, Object> request) {
        return Contact.existing(
            contact.id(),
            contact.clientId(),
            (String) request.getOrDefault("externalId", contact.externalId()),
            (String) request.getOrDefault("displayName", contact.displayName()),
            (String) request.getOrDefault("firstName", contact.firstName()),
            (String) request.getOrDefault("lastName", contact.lastName()),
            (String) request.getOrDefault("middleName", contact.middleName()),
            (String) request.getOrDefault("title", contact.title()),
            (String) request.getOrDefault("gender", contact.gender()),
            parseInstant(request.get("birthDate"), contact.birthDate()),
            (String) request.getOrDefault("nationality", contact.nationality()),
            (String) request.getOrDefault("documentType", contact.documentType()),
            (String) request.getOrDefault("documentNumber", contact.documentNumber()),
            request.get("phone") != null ? new PhoneE164((String) request.get("phone")) : contact.phoneE164(),
            (String) request.getOrDefault("phoneCountryCode", contact.phoneCountryCode()),
            request.get("email") != null ? new Email((String) request.get("email")) : contact.email(),
            request.get("secondaryEmail") != null ? new Email((String) request.get("secondaryEmail")) : contact.secondaryEmail(),
            (String) request.getOrDefault("addressLine1", contact.addressLine1()),
            (String) request.getOrDefault("addressLine2", contact.addressLine2()),
            (String) request.getOrDefault("city", contact.city()),
            (String) request.getOrDefault("stateProvince", contact.stateProvince()),
            (String) request.getOrDefault("postalCode", contact.postalCode()),
            (String) request.getOrDefault("country", contact.country()),
            (String) request.getOrDefault("timezone", contact.timezone()),
            (String) request.getOrDefault("locale", contact.locale()),
            (String) request.getOrDefault("preferredLanguage", contact.preferredLanguage()),
            (String) request.getOrDefault("companyName", contact.companyName()),
            (String) request.getOrDefault("jobTitle", contact.jobTitle()),
            (String) request.getOrDefault("department", contact.department()),
            (String) request.getOrDefault("website", contact.website()),
            (String) request.getOrDefault("linkedinProfile", contact.linkedinProfile()),
            (String) request.getOrDefault("twitterHandle", contact.twitterHandle()),
            (String) request.getOrDefault("facebookProfile", contact.facebookProfile()),
            (String) request.getOrDefault("instagramProfile", contact.instagramProfile()),
            (String) request.getOrDefault("emergencyContactName", contact.emergencyContactName()),
            (String) request.getOrDefault("emergencyContactPhone", contact.emergencyContactPhone()),
            (String) request.getOrDefault("emergencyContactRelationship", contact.emergencyContactRelationship()),
            (String) request.getOrDefault("notes", contact.notes()),
            (Boolean) request.getOrDefault("isVip", contact.isVip()),
            (Boolean) request.getOrDefault("isBlocked", contact.isBlocked()),
            (Boolean) request.getOrDefault("isActive", contact.isActive()),
            (String) request.getOrDefault("preferredContactMethod", contact.preferredContactMethod()),
            (String) request.getOrDefault("preferredContactTime", contact.preferredContactTime()),
            (Boolean) request.getOrDefault("marketingConsent", contact.marketingConsent()),
            (Boolean) request.getOrDefault("dataProcessingConsent", contact.dataProcessingConsent()),
            contact.tagNames(),
            request.get("attributes") != null ? (Map<String, Object>) request.get("attributes") : contact.attributes(),
            contact.lastSeenAt(),
            contact.lastContactedAt(),
            contact.totalInteractions(),
            contact.createdAt(),
            Instant.now()
        );
    }
    
    private Instant parseInstant(Object value, Instant defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof String) {
            try {
                return Instant.parse((String) value);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private Map<String, Object> toBasicContactDto(Contact contact) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", contact.id().value().toString());
        dto.put("displayName", contact.displayName());
        dto.put("fullName", contact.getFullName());
        dto.put("email", contact.email() != null ? contact.email().value() : null);
        dto.put("phone", contact.phoneE164() != null ? contact.phoneE164().value() : null);
        dto.put("isVip", contact.isVip());
        dto.put("isActive", contact.isActive());
        dto.put("totalInteractions", contact.totalInteractions());
        dto.put("lastSeenAt", contact.lastSeenAt());
        dto.put("createdAt", contact.createdAt());
        return dto;
    }
    
    private Map<String, Object> toDetailedContactDto(Contact contact) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", contact.id().value().toString());
        dto.put("clientId", contact.clientId().value().toString());
        dto.put("displayName", contact.displayName());
        dto.put("firstName", contact.firstName());
        dto.put("lastName", contact.lastName());
        dto.put("email", contact.email() != null ? contact.email().value() : null);
        dto.put("phone", contact.phoneE164() != null ? contact.phoneE164().value() : null);
        dto.put("isVip", contact.isVip());
        dto.put("isActive", contact.isActive());
        dto.put("tags", contact.tagNames());
        dto.put("createdAt", contact.createdAt());
        dto.put("updatedAt", contact.updatedAt());
        return dto;
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
    
    private Map<String, Object> toTemplateDto(WhatsAppTemplate template) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", template.id().value().toString());
        dto.put("name", template.name());
        dto.put("category", template.category().name());
        dto.put("status", template.status().name());
        dto.put("language", template.language());
        dto.put("parameterFormat", template.parameterFormat() != null ? template.parameterFormat().name() : null);
        dto.put("qualityRating", template.qualityRating().name());
        dto.put("canBeSent", template.canBeSent());
        dto.put("isSyncedWithMeta", template.isSyncedWithMeta());
        dto.put("createdAt", template.createdAt());
        dto.put("updatedAt", template.updatedAt());
        
        if (template.metaTemplateId() != null) {
            dto.put("metaTemplateId", template.metaTemplateId());
        }
        
        // Agregar información de rechazo si existe
        if (template.rejectionReason() != null) {
            dto.put("rejectionReason", template.rejectionReason());
        }
        if (template.rejectionCode() != null) {
            dto.put("rejectionCode", template.rejectionCode());
        }
        if (template.rejectionDetails() != null && !template.rejectionDetails().isEmpty()) {
            dto.put("rejectionDetails", template.rejectionDetails());
        }
        if (template.rejectedAt() != null) {
            dto.put("rejectedAt", template.rejectedAt());
        }
        
        return dto;
    }
    
    private Map<String, Object> toTemplateDtoWithComponents(WhatsAppTemplate template) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", template.id().value().toString());
        dto.put("name", template.name());
        dto.put("category", template.category().name());
        dto.put("status", template.status().name());
        dto.put("language", template.language());
        dto.put("parameterFormat", template.parameterFormat() != null ? template.parameterFormat().name() : null);
        dto.put("qualityRating", template.qualityRating().name());
        dto.put("canBeSent", template.canBeSent());
        dto.put("isSyncedWithMeta", template.isSyncedWithMeta());
        dto.put("components", template.components());
        dto.put("createdAt", template.createdAt());
        dto.put("updatedAt", template.updatedAt());
        
        if (template.metaTemplateId() != null) {
            dto.put("metaTemplateId", template.metaTemplateId());
        }
        
        // Agregar información de rechazo si existe
        if (template.rejectionReason() != null) {
            dto.put("rejectionReason", template.rejectionReason());
        }
        if (template.rejectionCode() != null) {
            dto.put("rejectionCode", template.rejectionCode());
        }
        if (template.rejectionDetails() != null && !template.rejectionDetails().isEmpty()) {
            dto.put("rejectionDetails", template.rejectionDetails());
        }
        if (template.rejectedAt() != null) {
            dto.put("rejectedAt", template.rejectedAt());
        }
        
        return dto;
    }
    
    /**
     * Parsea componentes desde el request JSON
     * Reutiliza la lógica de WhatsAppTemplateController
     */
    @SuppressWarnings("unchecked")
    private List<TemplateComponent> parseTemplateComponents(List<Map<String, Object>> componentsData) {
        return componentsData.stream()
            .map(componentMap -> {
                ComponentType type = ComponentType.valueOf((String) componentMap.get("type"));
                String text = (String) componentMap.get("text");
                
                // Parsear parámetros
                List<ComponentParameter> parameters = null;
                if (componentMap.containsKey("parameters")) {
                    List<Map<String, Object>> paramsData = (List<Map<String, Object>>) componentMap.get("parameters");
                    parameters = paramsData.stream()
                        .map(paramMap -> new ComponentParameter(
                            (String) paramMap.get("type"),
                            (String) paramMap.get("text"),
                            (String) paramMap.get("parameterName"),
                            (String) paramMap.get("example")
                        ))
                        .collect(Collectors.toList());
                }
                
                // Parsear botones
                List<TemplateButton> buttons = null;
                if (componentMap.containsKey("buttons")) {
                    List<Map<String, Object>> buttonsData = (List<Map<String, Object>>) componentMap.get("buttons");
                    buttons = buttonsData.stream()
                        .map(buttonMap -> new TemplateButton(
                            (String) buttonMap.get("type"),
                            (String) buttonMap.get("text"),
                            (String) buttonMap.get("url"),
                            (String) buttonMap.get("phoneNumber"),
                            (String) buttonMap.get("otpType"),
                            (String) buttonMap.get("autofillText"),
                            (String) buttonMap.get("packageName"),
                            (String) buttonMap.get("signatureHash"),
                            (String) buttonMap.get("example")
                        ))
                        .collect(Collectors.toList());
                }
                
                // Parsear media
                MediaComponent media = null;
                if (componentMap.containsKey("media")) {
                    Map<String, Object> mediaMap = (Map<String, Object>) componentMap.get("media");
                    media = new MediaComponent(
                        (String) mediaMap.get("type"),
                        (String) mediaMap.get("url"),
                        (String) mediaMap.get("mediaId"),
                        (String) mediaMap.get("filename"),
                        (String) mediaMap.get("altText")
                    );
                }
                
                // Nuevos campos de TemplateComponent
                String format = (String) componentMap.get("format");
                Boolean addSecurityRecommendation = componentMap.get("addSecurityRecommendation") != null ?
                    (Boolean) componentMap.get("addSecurityRecommendation") : null;
                Integer codeExpirationMinutes = componentMap.get("codeExpirationMinutes") != null ?
                    (Integer) componentMap.get("codeExpirationMinutes") : null;
                
                return new TemplateComponent(type, text, parameters, buttons, media, format,
                    addSecurityRecommendation, codeExpirationMinutes);
            })
            .collect(Collectors.toList());
    }
    
    @SuppressWarnings("unchecked")
    private SendBulkTemplate.BulkSendFilters parseBulkFilters(Map<String, Object> filtersMap) {
        Boolean onlyActive = (Boolean) filtersMap.getOrDefault("onlyActive", true);
        Boolean onlyVip = (Boolean) filtersMap.get("onlyVip");
        List<String> tagNames = (List<String>) filtersMap.get("tagNames");
        String preferredContactMethod = (String) filtersMap.getOrDefault("preferredContactMethod", "WHATSAPP");
        Boolean marketingConsent = (Boolean) filtersMap.getOrDefault("marketingConsent", true);
        
        return new SendBulkTemplate.BulkSendFilters(
            onlyActive,
            onlyVip,
            tagNames,
            null, // categoryIds
            preferredContactMethod,
            marketingConsent
        );
    }
}

