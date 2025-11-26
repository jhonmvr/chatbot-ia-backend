package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.ports.identity.ClientRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactRepository;
import com.relative.chat.bot.ia.domain.vo.Email;
import com.relative.chat.bot.ia.domain.vo.PhoneE164;
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
 * API REST para gestión de contactos
 */
@Slf4j
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "API para gestionar contactos con información completa de persona")
public class ContactController {
    
    private final ContactRepository contactRepository;
    private final ClientRepository clientRepository;
    
    /**
     * Crear un nuevo contacto
     * POST /api/contacts
     */
    @Operation(
        summary = "Crear nuevo contacto",
        description = "Crea un nuevo contacto con información básica de persona"
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
                      "message": "clientId, displayName, firstName, lastName, phone y email son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createContact(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del contacto a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientId": "a1234567-e89b-12d3-a456-426614174000",
                      "displayName": "Juan Pérez",
                      "firstName": "Juan",
                      "lastName": "Pérez",
                      "middleName": "Carlos",
                      "title": "Sr.",
                      "gender": "MALE",
                      "birthDate": "1990-05-15T00:00:00Z",
                      "nationality": "Mexicana",
                      "documentType": "DNI",
                      "documentNumber": "12345678",
                      "phone": "+525512345678",
                      "phoneCountryCode": "+52",
                      "email": "juan.perez@email.com",
                      "secondaryEmail": "juan.carlos@work.com",
                      "addressLine1": "Av. Reforma 123",
                      "addressLine2": "Col. Centro",
                      "city": "Ciudad de México",
                      "stateProvince": "CDMX",
                      "postalCode": "06000",
                      "country": "México",
                      "timezone": "America/Mexico_City",
                      "locale": "es_MX",
                      "preferredLanguage": "es",
                      "companyName": "Tech Solutions",
                      "jobTitle": "Desarrollador Senior",
                      "department": "IT",
                      "website": "https://juanperez.com",
                      "linkedinProfile": "https://linkedin.com/in/juanperez",
                      "twitterHandle": "@juanperez",
                      "facebookProfile": "https://facebook.com/juanperez",
                      "instagramProfile": "https://instagram.com/juanperez",
                      "emergencyContactName": "María Pérez",
                      "emergencyContactPhone": "+525512345679",
                      "emergencyContactRelationship": "Esposa",
                      "notes": "Cliente VIP - Preferencia por WhatsApp",
                      "isVip": true,
                      "preferredContactMethod": "WHATSAPP",
                      "preferredContactTime": "AFTERNOON",
                      "marketingConsent": true,
                      "dataProcessingConsent": true,
                      "tags": ["VIP", "TECH", "PREMIUM"]
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            // Validar campos requeridos
            String clientId = (String) request.get("clientId");
            String displayName = (String) request.get("displayName");
            String firstName = (String) request.get("firstName");
            String lastName = (String) request.get("lastName");
            String phone = (String) request.get("phone");
            String email = (String) request.get("email");
            
            if (clientId == null || displayName == null || firstName == null || lastName == null || phone == null || email == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientId, displayName, firstName, lastName, phone y email son requeridos"
                ));
            }
            
            // Verificar que el cliente existe
            if (clientRepository.findById(UuidId.of(UUID.fromString(clientId))).isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Cliente no encontrado: " + clientId
                ));
            }
            
            // Crear contacto
            Contact contact = Contact.create(
                UuidId.of(UUID.fromString(clientId)),
                displayName,
                firstName,
                lastName,
                new PhoneE164(phone),
                new Email(email)
            );
            
            // Actualizar con datos adicionales si se proporcionan
            contact = updateContactWithAdditionalData(contact, request);
            
            // Guardar
            contactRepository.save(contact);
            
            log.info("Contacto creado exitosamente para cliente: {}", clientId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "contactId", contact.id().value().toString(),
                "message", "Contacto creado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al crear contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al crear contacto: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Listar contactos por cliente
     * GET /api/contacts/client/{clientId}
     */
    @Operation(
        summary = "Listar contactos por cliente",
        description = "Obtiene todos los contactos de un cliente específico"
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
                          "lastSeenAt": "2024-12-19T10:30:00Z"
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> getContactsByClient(
        @Parameter(description = "UUID del cliente", required = true)
        @PathVariable String clientId
    ) {
        try {
            // Verificar que el cliente existe
            if (clientRepository.findById(UuidId.of(UUID.fromString(clientId))).isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Cliente no encontrado: " + clientId
                ));
            }
            
            List<Contact> contacts = contactRepository.findByClientId(UuidId.of(UUID.fromString(clientId)));
            
            List<Map<String, Object>> contactDtos = contacts.stream()
                .map(this::toBasicDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "contacts", contactDtos
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener contactos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener contactos: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtener contacto por ID
     * GET /api/contacts/{id}
     */
    @Operation(
        summary = "Obtener contacto por ID",
        description = "Obtiene un contacto específico con toda su información"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contacto obtenido exitosamente"
        ),
        @ApiResponse(responseCode = "404", description = "Contacto no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getContactById(
        @Parameter(description = "UUID del contacto", required = true)
        @PathVariable String id
    ) {
        try {
            Optional<Contact> contactOpt = contactRepository.findById(UuidId.of(UUID.fromString(id)));
            
            if (contactOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Contacto no encontrado: " + id
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "contact", toDetailedDto(contactOpt.get())
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener contacto: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Actualizar contacto
     * PUT /api/contacts/{id}
     */
    @Operation(
        summary = "Actualizar contacto",
        description = "Actualiza la información de un contacto existente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contacto actualizado exitosamente"
        ),
        @ApiResponse(responseCode = "404", description = "Contacto no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateContact(
        @Parameter(description = "UUID del contacto a actualizar", required = true)
        @PathVariable String id,
        @RequestBody Map<String, Object> request
    ) {
        try {
            Optional<Contact> existingContactOpt = contactRepository.findById(UuidId.of(UUID.fromString(id)));
            
            if (existingContactOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Contacto no encontrado: " + id
                ));
            }
            
            Contact existingContact = existingContactOpt.get();
            
            // Actualizar contacto con nuevos datos
            Contact updatedContact = updateContactWithAdditionalData(existingContact, request);
            
            // Guardar
            contactRepository.save(updatedContact);
            
            log.info("Contacto actualizado exitosamente: {}", id);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "contactId", updatedContact.id().value().toString(),
                "message", "Contacto actualizado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al actualizar contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al actualizar contacto: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Eliminar contacto
     * DELETE /api/contacts/{id}
     */
    @Operation(
        summary = "Eliminar contacto",
        description = "Elimina un contacto de la base de datos"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contacto eliminado exitosamente"
        ),
        @ApiResponse(responseCode = "404", description = "Contacto no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteContact(
        @Parameter(description = "UUID del contacto a eliminar", required = true)
        @PathVariable String id
    ) {
        try {
            if (contactRepository.findById(UuidId.of(UUID.fromString(id))).isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Contacto no encontrado: " + id
                ));
            }
            
            contactRepository.delete(UuidId.of(UUID.fromString(id)));
            
            log.info("Contacto eliminado exitosamente: {}", id);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Contacto eliminado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al eliminar contacto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al eliminar contacto: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Buscar contactos por criterios
     * GET /api/contacts/search
     */
    @Operation(
        summary = "Buscar contactos",
        description = "Busca contactos con filtros opcionales. Si no se especifican criterios, devuelve todos los contactos"
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
                      "contacts": [...],
                      "total": 150,
                      "page": 0,
                      "size": 20,
                      "totalPages": 8
                    }
                    """)
            )
        )
    })
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchContacts(
        @Parameter(description = "Texto de búsqueda (nombre, email, teléfono)")
        @RequestParam(required = false) String query,
        
        @Parameter(description = "UUID del cliente para filtrar")
        @RequestParam(required = false) String clientId,
        
        @Parameter(description = "Filtrar por VIP")
        @RequestParam(required = false) Boolean isVip,
        
        @Parameter(description = "Filtrar por activo")
        @RequestParam(required = false) Boolean isActive,
        
        @Parameter(description = "Etiqueta para filtrar")
        @RequestParam(required = false) String tag,
        
        @Parameter(description = "Número de página (0-indexed)")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Tamaño de página")
        @RequestParam(defaultValue = "20") int size
        ) {
        try {
            com.relative.chat.bot.ia.domain.common.UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientUuidId = clientId != null ? UuidId.of(UUID.fromString(clientId)) : null;
            
            // Buscar contactos directamente en la base de datos
            ContactRepository.SearchResult searchResult = contactRepository.searchContacts(
                clientUuidId,
                query,
                isVip,
                isActive,
                tag,
                page,
                size
            );
            
            // Convertir a DTOs
            List<Map<String, Object>> contactDtos = searchResult.contacts().stream()
                .map(this::toBasicDto)
                .collect(Collectors.toList());
            
            log.info("Búsqueda de contactos: {} contactos encontrados, página {}, tamaño {}", 
                searchResult.total(), page, size);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "contacts", contactDtos,
                "total", searchResult.total(),
                "page", searchResult.page(),
                "size", searchResult.size(),
                "totalPages", searchResult.totalPages()
            ));
            
        } catch (Exception e) {
            log.error("Error al buscar contactos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al buscar contactos: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtener estadísticas de contactos
     * GET /api/contacts/stats/client/{clientId}
     */
    @Operation(
        summary = "Obtener estadísticas de contactos",
        description = "Obtiene estadísticas agregadas de los contactos de un cliente específico, incluyendo totales, VIPs, activos, bloqueados e interacciones"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Estadísticas obtenidas exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "stats": {
                        "totalContacts": 150,
                        "vipContacts": 25,
                        "activeContacts": 140,
                        "blockedContacts": 5,
                        "totalInteractions": 1250
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "ID de cliente inválido"),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/stats/client/{clientId}")
    public ResponseEntity<Map<String, Object>> getContactStats(
        @Parameter(description = "UUID del cliente", required = true, example = "a1234567-e89b-12d3-a456-426614174000")
        @PathVariable String clientId
    ) {
        try {
            List<Contact> contacts = contactRepository.findByClientId(UuidId.of(UUID.fromString(clientId)));
            
            long totalContacts = contacts.size();
            long vipContacts = contacts.stream().mapToLong(c -> c.isVip() ? 1 : 0).sum();
            long activeContacts = contacts.stream().mapToLong(c -> c.isActive() ? 1 : 0).sum();
            long blockedContacts = contacts.stream().mapToLong(c -> c.isBlocked() ? 1 : 0).sum();
            
            int totalInteractions = contacts.stream()
                .mapToInt(c -> c.totalInteractions() != null ? c.totalInteractions() : 0)
                .sum();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "stats", Map.of(
                    "totalContacts", totalContacts,
                    "vipContacts", vipContacts,
                    "activeContacts", activeContacts,
                    "blockedContacts", blockedContacts,
                    "totalInteractions", totalInteractions
                )
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener estadísticas: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Actualiza un contacto con datos adicionales del request
     */
    @SuppressWarnings("unchecked")
    private Contact updateContactWithAdditionalData(Contact contact, Map<String, Object> request) {
        // Crear contacto actualizado con todos los campos
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
            Instant.now() // updatedAt
        );
    }
    
    /**
     * Parsea un objeto a Instant
     */
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
    
    /**
     * Convierte un Contact a DTO básico
     */
    private Map<String, Object> toBasicDto(Contact contact) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", contact.id().value().toString());
        dto.put("displayName", contact.displayName());
        dto.put("fullName", contact.getFullName());
        dto.put("email", contact.email() != null ? contact.email().value() : null);
        dto.put("phone", contact.phoneE164() != null ? contact.phoneE164().value() : null);
        dto.put("isVip", contact.isVip());
        dto.put("isActive", contact.isActive());
        dto.put("isBlocked", contact.isBlocked());
        dto.put("totalInteractions", contact.totalInteractions());
        dto.put("lastSeenAt", contact.lastSeenAt());
        dto.put("createdAt", contact.createdAt());
        return dto;
    }
    
    /**
     * Convierte un Contact a DTO detallado
     */
    private Map<String, Object> toDetailedDto(Contact contact) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", contact.id().value().toString());
        dto.put("clientId", contact.clientId().value().toString());
        dto.put("externalId", contact.externalId());
        dto.put("displayName", contact.displayName());
        dto.put("firstName", contact.firstName());
        dto.put("lastName", contact.lastName());
        dto.put("middleName", contact.middleName());
        dto.put("title", contact.title());
        dto.put("gender", contact.gender());
        dto.put("birthDate", contact.birthDate());
        dto.put("nationality", contact.nationality());
        dto.put("documentType", contact.documentType());
        dto.put("documentNumber", contact.documentNumber());
        dto.put("phone", contact.phoneE164() != null ? contact.phoneE164().value() : null);
        dto.put("phoneCountryCode", contact.phoneCountryCode());
        dto.put("email", contact.email() != null ? contact.email().value() : null);
        dto.put("secondaryEmail", contact.secondaryEmail() != null ? contact.secondaryEmail().value() : null);
        dto.put("addressLine1", contact.addressLine1());
        dto.put("addressLine2", contact.addressLine2());
        dto.put("city", contact.city());
        dto.put("stateProvince", contact.stateProvince());
        dto.put("postalCode", contact.postalCode());
        dto.put("country", contact.country());
        dto.put("fullAddress", contact.getFullAddress());
        dto.put("timezone", contact.timezone());
        dto.put("locale", contact.locale());
        dto.put("preferredLanguage", contact.preferredLanguage());
        dto.put("companyName", contact.companyName());
        dto.put("jobTitle", contact.jobTitle());
        dto.put("department", contact.department());
        dto.put("website", contact.website());
        dto.put("linkedinProfile", contact.linkedinProfile());
        dto.put("twitterHandle", contact.twitterHandle());
        dto.put("facebookProfile", contact.facebookProfile());
        dto.put("instagramProfile", contact.instagramProfile());
        dto.put("emergencyContactName", contact.emergencyContactName());
        dto.put("emergencyContactPhone", contact.emergencyContactPhone());
        dto.put("emergencyContactRelationship", contact.emergencyContactRelationship());
        dto.put("notes", contact.notes());
        dto.put("isVip", contact.isVip());
        dto.put("isBlocked", contact.isBlocked());
        dto.put("isActive", contact.isActive());
        dto.put("preferredContactMethod", contact.preferredContactMethod());
        dto.put("preferredContactTime", contact.preferredContactTime());
        dto.put("marketingConsent", contact.marketingConsent());
        dto.put("dataProcessingConsent", contact.dataProcessingConsent());
        dto.put("tags", contact.tagNames());
        dto.put("attributes", contact.attributes());
        dto.put("lastSeenAt", contact.lastSeenAt());
        dto.put("lastContactedAt", contact.lastContactedAt());
        dto.put("totalInteractions", contact.totalInteractions());
        dto.put("createdAt", contact.createdAt());
        dto.put("updatedAt", contact.updatedAt());
        return dto;
    }
}
