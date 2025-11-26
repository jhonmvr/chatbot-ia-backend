package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.usecases.SendBulkTemplate;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.*;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * API REST para envío masivo de mensajes con plantillas
 * 
 * Basado en la documentación de WhatsApp Business Management API:
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/message-templates/template-messaging-limits
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/message-templates/template-pacing
 * - https://developers.facebook.com/docs/whatsapp/business-management-api/message-templates/template-pausing
 */
@Slf4j
@RestController
@RequestMapping("/api/templates/send/bulk")
@RequiredArgsConstructor
@Tag(name = "Bulk Template Sending", description = "API para envío masivo de mensajes con plantillas")
public class SendBulkTemplateController {
    
    private final SendBulkTemplate sendBulkTemplate;
    private final ClientPhoneRepository clientPhoneRepository;
    
    /**
     * Envío masivo de plantillas con filtros
     * POST /api/templates/send/bulk/filtered
     */
    @Operation(
        summary = "Envío masivo con filtros",
        description = "Envía plantillas a múltiples contactos usando filtros específicos"
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
                        "messages": [],
                        "startedAt": "2024-12-19T10:30:00Z",
                        "completedAt": "2024-12-19T10:35:00Z"
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o límites excedidos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Límite de envío masivo excedido. Máximo: 1000, Solicitado: 1500"
                    }
                    """)
            )
        )
    })
    @PostMapping(value = "/filtered", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendBulkTemplateFiltered(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos para envío masivo con filtros",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientId": "a1234567-e89b-12d3-a456-426614174000",
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
            // Validar campos requeridos
            String clientId = (String) request.get("clientId");
            String phoneId = (String) request.get("phoneId");
            String templateName = (String) request.get("templateName");
            String parameterFormatStr = (String) request.getOrDefault("parameterFormat", "NAMED");
            
            if (clientId == null || phoneId == null || templateName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientId, phoneId y templateName son requeridos"
                ));
            }
            
            // Verificar que el teléfono existe
            if (clientPhoneRepository.findById(UuidId.of(UUID.fromString(phoneId))).isEmpty()) {
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
            
            // Parsear filtros
            @SuppressWarnings("unchecked")
            Map<String, Object> filtersMap = (Map<String, Object>) request.getOrDefault("filters", new HashMap<>());
            SendBulkTemplate.BulkSendFilters filters = parseFilters(filtersMap);
            
            // Ejecutar envío masivo
            SendBulkTemplate.BulkSendResult result = sendBulkTemplate.handle(
                    UuidId.of(UUID.fromString(clientId)),
                    UuidId.of(UUID.fromString(phoneId)),
                    templateName,
                    parameters,
                    parameterFormat,
                    filters
            );
            
            log.info("Envío masivo completado. Total: {}, Exitosos: {}, Fallidos: {}", 
                    result.totalContacts(), result.successfulSends(), result.failedSends());
            
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
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error en envío masivo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error en envío masivo: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Envío masivo a contactos VIP
     * POST /api/templates/send/bulk/vip
     */
    @Operation(
        summary = "Envío masivo a contactos VIP",
        description = "Envía plantillas específicamente a contactos marcados como VIP. Solo se enviarán a contactos activos con consentimiento de marketing."
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
                        "errors": ["Error en contacto xyz: Template no aprobado"],
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
                      "message": "clientId, phoneId y templateName son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Teléfono no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(value = "/vip", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendBulkTemplateToVip(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos para envío masivo a contactos VIP",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientId": "a1234567-e89b-12d3-a456-426614174000",
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
            // Validar campos requeridos
            String clientId = (String) request.get("clientId");
            String phoneId = (String) request.get("phoneId");
            String templateName = (String) request.get("templateName");
            
            if (clientId == null || phoneId == null || templateName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientId, phoneId y templateName son requeridos"
                ));
            }
            
            // Parsear parámetros
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) request.getOrDefault("parameters", new HashMap<>());
            
            // Usar filtros para VIP
            SendBulkTemplate.BulkSendFilters vipFilters = SendBulkTemplate.BulkSendFilters.forVipContacts();
            
            // Ejecutar envío masivo
            SendBulkTemplate.BulkSendResult result = sendBulkTemplate.handle(
                    UuidId.of(UUID.fromString(clientId)),
                    UuidId.of(UUID.fromString(phoneId)),
                    templateName,
                    parameters,
                    ParameterFormat.NAMED,
                    vipFilters
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
                "message", "Error en envío masivo VIP: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Envío masivo por tags
     * POST /api/templates/send/bulk/by-tags
     */
    @Operation(
        summary = "Envío masivo por tags",
        description = "Envía plantillas a contactos que tienen uno o más tags específicos. Los contactos deben tener al menos uno de los tags especificados y estar activos con consentimiento de marketing."
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
                      "message": "tagNames es requerido y no puede estar vacío"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Teléfono no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(value = "/by-tags", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendBulkTemplateByTags(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos para envío masivo por tags",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientId": "a1234567-e89b-12d3-a456-426614174000",
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
            // Validar campos requeridos
            String clientId = (String) request.get("clientId");
            String phoneId = (String) request.get("phoneId");
            String templateName = (String) request.get("templateName");
            
            if (clientId == null || phoneId == null || templateName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientId, phoneId y templateName son requeridos"
                ));
            }
            
            // Parsear tags
            @SuppressWarnings("unchecked")
            List<String> tagNames = (List<String>) request.get("tagNames");
            
            if (tagNames == null || tagNames.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "tagNames es requerido y no puede estar vacío"
                ));
            }
            
            // Parsear parámetros
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) request.getOrDefault("parameters", new HashMap<>());
            
            // Usar filtros para tags
            SendBulkTemplate.BulkSendFilters tagFilters = SendBulkTemplate.BulkSendFilters.forTaggedContacts(tagNames);
            
            // Ejecutar envío masivo
            SendBulkTemplate.BulkSendResult result = sendBulkTemplate.handle(
                    UuidId.of(UUID.fromString(clientId)),
                    UuidId.of(UUID.fromString(phoneId)),
                    templateName,
                    parameters,
                    ParameterFormat.NAMED,
                    tagFilters
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
                "message", "Error en envío masivo por tags: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Envío masivo a lista específica de contactos
     * POST /api/templates/send/bulk/by-contacts
     */
    @Operation(
        summary = "Envío masivo a lista de contactos",
        description = "Envía plantillas a una lista específica de contactos identificados por sus UUIDs. Útil cuando se necesita enviar a un grupo seleccionado manualmente de contactos."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Envío masivo a lista de contactos completado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "result": {
                        "totalContacts": 10,
                        "successfulSends": 9,
                        "failedSends": 1,
                        "errors": ["Error en contacto xyz: Contacto no encontrado"],
                        "startedAt": "2024-12-19T10:30:00Z",
                        "completedAt": "2024-12-19T10:31:00Z"
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
                      "message": "contactIds es requerido y no puede estar vacío"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Teléfono no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping(value = "/by-contacts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendBulkTemplateByContacts(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos para envío masivo a lista específica de contactos",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientId": "a1234567-e89b-12d3-a456-426614174000",
                      "phoneId": "b1234567-e89b-12d3-a456-426614174000",
                      "templateName": "custom_promotion",
                      "contactIds": [
                        "c1234567-e89b-12d3-a456-426614174000",
                        "d1234567-e89b-12d3-a456-426614174000"
                      ],
                      "parameters": {
                        "product_name": "Producto Especial"
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
            String templateName = (String) request.get("templateName");
            
            if (clientId == null || phoneId == null || templateName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientId, phoneId y templateName son requeridos"
                ));
            }
            
            // Parsear lista de contactos
            @SuppressWarnings("unchecked")
            List<String> contactIds = (List<String>) request.get("contactIds");
            
            if (contactIds == null || contactIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "contactIds es requerido y no puede estar vacío"
                ));
            }
            
            // Parsear parámetros
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) request.getOrDefault("parameters", new HashMap<>());
            
            // TODO: Implementar envío a lista específica de contactos
            // Por ahora, usar filtros por defecto
            SendBulkTemplate.BulkSendFilters defaultFilters = SendBulkTemplate.BulkSendFilters.createDefault();
            
            // Ejecutar envío masivo
            SendBulkTemplate.BulkSendResult result = sendBulkTemplate.handle(
                    UuidId.of(UUID.fromString(clientId)),
                    UuidId.of(UUID.fromString(phoneId)),
                    templateName,
                    parameters,
                    ParameterFormat.NAMED,
                    defaultFilters
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
            log.error("Error en envío masivo por contactos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error en envío masivo por contactos: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Parsear filtros desde el request
     */
    @SuppressWarnings("unchecked")
    private SendBulkTemplate.BulkSendFilters parseFilters(Map<String, Object> filtersMap) {
        Boolean onlyActive = (Boolean) filtersMap.getOrDefault("onlyActive", true);
        Boolean onlyVip = (Boolean) filtersMap.get("onlyVip");
        List<String> tagNames = (List<String>) filtersMap.get("tagNames");
        List<String> categoryIdsStr = (List<String>) filtersMap.get("categoryIds");
        String preferredContactMethod = (String) filtersMap.getOrDefault("preferredContactMethod", "WHATSAPP");
        Boolean marketingConsent = (Boolean) filtersMap.getOrDefault("marketingConsent", true);
        
        // Convertir categoryIds de String a UuidId
        List<UuidId<Category>> categoryIds = null;
        if (categoryIdsStr != null && !categoryIdsStr.isEmpty()) {
            categoryIds = categoryIdsStr.stream()
                    .map(id -> UuidId.<Category>of(UUID.fromString(id)))
                    .collect(Collectors.toList());
        }
        
        return new SendBulkTemplate.BulkSendFilters(
                onlyActive,
                onlyVip,
                tagNames,
                categoryIds,
                preferredContactMethod,
                marketingConsent
        );
    }
}
