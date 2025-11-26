package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.services.WhatsAppTemplateService;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.*;
import com.relative.chat.bot.ia.domain.ports.messaging.WhatsAppTemplateRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API REST para gestión de plantillas de WhatsApp
 */
@Slf4j
@RestController
@RequestMapping("/api/whatsapp-templates")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Templates", description = "API para gestionar plantillas de WhatsApp Business API con sincronización a Meta")
public class WhatsAppTemplateController {
    
    private final WhatsAppTemplateService templateService;
    private final WhatsAppTemplateRepository templateRepository;
    
    /**
     * Crear una nueva plantilla de WhatsApp
     * POST /api/whatsapp-templates
     */
    @Operation(
        summary = "Crear plantilla de WhatsApp",
        description = "Crea una nueva plantilla de WhatsApp con componentes específicos según la categoría"
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
        @ApiResponse(responseCode = "409", description = "Plantilla con el mismo nombre ya existe")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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
                          "text": "Tu código de verificación es: {{1}}. Este código expira en {{2}} minutos.",
                          "parameters": [
                            {
                              "type": "text",
                              "text": "123456",
                              "example": "123456"
                            },
                            {
                              "type": "text",
                              "text": "5",
                              "example": "5"
                            }
                          ]
                        },
                        {
                          "type": "BUTTONS",
                          "buttons": [
                            {
                              "type": "QUICK_REPLY",
                              "text": "Copiar código"
                            }
                          ]
                        }
                      ]
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            // Validar campos requeridos
            String clientPhoneId = (String) request.get("clientPhoneId");
            String name = (String) request.get("name");
            String category = (String) request.get("category");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> componentsData = (List<Map<String, Object>>) request.get("components");
            
            if (clientPhoneId == null || name == null || category == null || componentsData == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientPhoneId, name, category y components son requeridos"
                ));
            }
            
            // Obtener valores opcionales
            String language = (String) request.getOrDefault("language", "es_ES");
            String parameterFormat = (String) request.getOrDefault("parameterFormat", "POSITIONAL");
            
            // Convertir componentes
            List<TemplateComponent> components = parseComponents(componentsData);
            
            // Crear plantilla
            WhatsAppTemplate template = templateService.createTemplate(
                UuidId.of(UUID.fromString(clientPhoneId)),
                name,
                TemplateCategory.valueOf(category),
                language,
                ParameterFormat.valueOf(parameterFormat),
                components
            );
            
            log.info("Plantilla creada exitosamente: {} para cliente: {}", template.id().value(), clientPhoneId);
            
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

    /**
     * Listar plantillas por cliente
     * GET /api/whatsapp-templates/client/{clientPhoneId}
     */
    @Operation(
        summary = "Listar plantillas por cliente",
        description = "Obtiene todas las plantillas de un número de teléfono cliente específico"
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
                          "status": "APPROVED",
                          "language": "es_ES",
                          "qualityRating": "HIGH",
                          "isSyncedWithMeta": true,
                          "metaTemplateId": "123456789012345"
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/client/{clientPhoneId}")
    public ResponseEntity<Map<String, Object>> getTemplatesByClient(
        @Parameter(description = "UUID del número de teléfono cliente", required = true)
        @PathVariable String clientPhoneId
    ) {
        try {
            List<WhatsAppTemplate> templates = templateService.getTemplatesByClient(
                UuidId.of(UUID.fromString(clientPhoneId))
            );
            
            List<Map<String, Object>> templateDtos = templates.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "templates", templateDtos
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener plantillas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener plantillas: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtener una plantilla por su ID
     * GET /api/whatsapp-templates/{templateId}
     */
    @Operation(
            summary = "Obtener plantilla por ID",
            description = "Obtiene los detalles de una plantilla específica de WhatsApp usando su UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Plantilla encontrada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "template": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "name": "otp_verification",
                        "category": "AUTHENTICATION",
                        "status": "APPROVED",
                        "language": "es_ES",
                        "parameterFormat": "POSITIONAL",
                        "components": [
                          {
                            "type": "BODY",
                            "text": "Tu código de verificación es: {{1}}"
                          }
                        ],
                        "metaTemplateId": "123456789012345",
                        "createdAt": "2025-11-10T12:34:56Z",
                        "updatedAt": "2025-11-10T12:34:56Z"
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
                      "message": "Plantilla no encontrada: 550e8400-e29b-41d4-a716-446655440000"
                    }
                    """)
                    )
            )
    })
    @GetMapping("/{templateId}")
    public ResponseEntity<Map<String, Object>> getTemplateById(
            @Parameter(description = "UUID de la plantilla de WhatsApp", required = true)
            @PathVariable String templateId
    ) {
        try {
            WhatsAppTemplate template = templateService.findById(UuidId.of(UUID.fromString(templateId)));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("template", toDtoWithComponents(template));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Plantilla no encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener plantilla: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener plantilla: " + e.getMessage()
            ));
        }
    }
    /**
     * Sincronizar plantilla con Meta API
     * POST /api/whatsapp-templates/{id}/sync
     */
    @Operation(
        summary = "Sincronizar plantilla con Meta",
        description = "Envía una plantilla a Meta WhatsApp Business API para su revisión y aprobación"
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
        @ApiResponse(responseCode = "400", description = "Plantilla no está en estado DRAFT")
    })
    @PostMapping("/{id}/sync")
    public ResponseEntity<Map<String, Object>> syncTemplateToMeta(
        @Parameter(description = "UUID de la plantilla a sincronizar", required = true)
        @PathVariable String id
    ) {
        try {
            WhatsAppTemplate syncedTemplate = templateService.syncTemplateToMeta(
                UuidId.of(UUID.fromString(id))
            );
            
            log.info("Plantilla sincronizada exitosamente: {} con Meta ID: {}", 
                syncedTemplate.id().value(), syncedTemplate.metaTemplateId());
            
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
    
    /**
     * Actualizar estado de plantilla desde Meta
     * POST /api/whatsapp-templates/{id}/update-status
     */
    @Operation(
        summary = "Actualizar estado desde Meta",
        description = "Actualiza el estado de una plantilla consultando directamente a Meta API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Estado actualizado exitosamente"
        ),
        @ApiResponse(responseCode = "400", description = "Plantilla no está sincronizada con Meta")
    })
    @PostMapping("/{id}/update-status")
    public ResponseEntity<Map<String, Object>> updateTemplateStatus(
        @Parameter(description = "UUID de la plantilla", required = true)
        @PathVariable String id
    ) {
        try {
            WhatsAppTemplate updatedTemplate = templateService.updateTemplateStatusFromMeta(
                UuidId.of(UUID.fromString(id))
            );
            
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
    
    /**
     * Eliminar plantilla
     * DELETE /api/whatsapp-templates/{id}
     */
    @Operation(
        summary = "Eliminar plantilla",
        description = "Elimina una plantilla tanto de la base de datos local como de Meta API"
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
        @ApiResponse(responseCode = "404", description = "Plantilla no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(
        @Parameter(description = "UUID de la plantilla a eliminar", required = true)
        @PathVariable String id
    ) {
        try {
            templateService.deleteTemplate(UuidId.of(UUID.fromString(id)));
            
            log.info("Plantilla eliminada exitosamente: {}", id);
            
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
    
    /**
     * Listar plantillas por estado (opcional)
     * GET /api/whatsapp-templates/status/{status}
     * Si status es "all" o no se proporciona, devuelve todas las plantillas
     */
    @Operation(
        summary = "Listar plantillas por estado",
        description = "Obtiene todas las plantillas filtradas por estado. Estados válidos: APPROVED, PENDING, REJECTED, DISABLED, DRAFT. Use 'all' o omita el parámetro para obtener todas las plantillas."
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
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping(value = {"/status/{status}", "/status"})
    public ResponseEntity<Map<String, Object>> getTemplatesByStatus(
        @Parameter(description = "Estado de la plantilla (opcional). Valores: APPROVED, PENDING, REJECTED, DISABLED, DRAFT, o 'all' para obtener todas", example = "APPROVED")
        @PathVariable(required = false) String status
    ) {
        try {
            List<WhatsAppTemplate> templates;
            
            // Si status es null, vacío o "all", obtener todas las plantillas
            if (status == null || status.trim().isEmpty() || status.equalsIgnoreCase("all")) {
                templates = templateRepository.findAll();
            } else {
                templates = templateService.getTemplatesByStatus(TemplateStatus.valueOf(status.toUpperCase()));
            }
            
            List<Map<String, Object>> templateDtos = templates.stream()
                .map(this::toDto)
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
    
    /**
     * Listar plantillas por categoría
     * GET /api/whatsapp-templates/category/{category}
     */
    @Operation(
        summary = "Listar plantillas por categoría",
        description = "Obtiene todas las plantillas de una categoría específica. Categorías válidas: AUTHENTICATION, MARKETING, UTILITY."
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
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getTemplatesByCategory(
        @Parameter(description = "Categoría de la plantilla. Valores: AUTHENTICATION, MARKETING, UTILITY", required = true, example = "AUTHENTICATION")
        @PathVariable String category
    ) {
        try {
            List<WhatsAppTemplate> templates = templateService.getTemplatesByCategory(
                TemplateCategory.valueOf(category)
            );
            
            List<Map<String, Object>> templateDtos = templates.stream()
                .map(this::toDto)
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
    
    /**
     * Obtener plantillas listas para usar
     * GET /api/whatsapp-templates/ready
     */
    @Operation(
        summary = "Obtener plantillas listas para usar",
        description = "Obtiene todas las plantillas que están aprobadas y listas para ser enviadas. Estas son plantillas con estado APPROVED y que pueden ser enviadas."
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
                          "canBeSent": true
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> getReadyTemplates() {
        try {
            List<WhatsAppTemplate> templates = templateService.getReadyTemplates();
            
            List<Map<String, Object>> templateDtos = templates.stream()
                .map(this::toDto)
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
    
    /**
     * Sincronizar todas las plantillas pendientes
     * POST /api/whatsapp-templates/sync-all
     */
    @Operation(
        summary = "Sincronizar todas las plantillas pendientes",
        description = "Sincroniza todas las plantillas que están en estado DRAFT con Meta WhatsApp Business API para su revisión y aprobación. Este proceso puede tomar tiempo dependiendo del número de plantillas."
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
        @ApiResponse(
            responseCode = "500",
            description = "Error al sincronizar plantillas",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Error al sincronizar plantillas: [detalle del error]"
                    }
                    """)
            )
        )
    })
    @PostMapping("/sync-all")
    public ResponseEntity<Map<String, Object>> syncAllPendingTemplates() {
        try {
            templateService.syncAllPendingTemplates();
            
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
    
    /**
     * Actualizar estados de todas las plantillas sincronizadas
     * POST /api/whatsapp-templates/update-all-statuses
     */
    @Operation(
        summary = "Actualizar estados de todas las plantillas sincronizadas",
        description = "Consulta Meta WhatsApp Business API para actualizar el estado de todas las plantillas que están sincronizadas con Meta. Esto actualiza estados como APPROVED, PENDING, REJECTED, etc."
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
        @ApiResponse(
            responseCode = "500",
            description = "Error al actualizar estados",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Error al actualizar estados: [detalle del error]"
                    }
                    """)
            )
        )
    })
    @PostMapping("/update-all-statuses")
    public ResponseEntity<Map<String, Object>> updateAllTemplateStatuses() {
        try {
            templateService.updateAllTemplateStatuses();
            
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
    
    /**
     * Convierte un WhatsAppTemplate del dominio a DTO
     */
    private Map<String, Object> toDto(WhatsAppTemplate template) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", template.id().value().toString());
        dto.put("clientPhoneId", template.clientPhoneId().value().toString());
        dto.put("name", template.name());
        dto.put("category", template.category().name());
        dto.put("language", template.language());
        dto.put("status", template.status().name());
        dto.put("parameterFormat", template.parameterFormat() != null ? template.parameterFormat().name() : null);
        dto.put("qualityRating", template.qualityRating().name());
        dto.put("isSyncedWithMeta", template.isSyncedWithMeta());
        dto.put("canBeSent", template.canBeSent());
        dto.put("createdAt", template.createdAt());
        dto.put("updatedAt", template.updatedAt());
        
        if (template.metaTemplateId() != null) {
            dto.put("metaTemplateId", template.metaTemplateId());
        }
        
        if (template.rejectionReason() != null) {
            dto.put("rejectionReason", template.rejectionReason());
        }
        
        return dto;
    }

    private Map<String, Object> toDtoWithComponents(WhatsAppTemplate template) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", template.id().value().toString());
        dto.put("clientPhoneId", template.clientPhoneId().value().toString());
        dto.put("name", template.name());
        dto.put("category", template.category().name());
        dto.put("language", template.language());
        dto.put("status", template.status().name());
        dto.put("parameterFormat", template.parameterFormat() != null ? template.parameterFormat().name() : null);
        dto.put("qualityRating", template.qualityRating().name());
        dto.put("isSyncedWithMeta", template.isSyncedWithMeta());
        dto.put("canBeSent", template.canBeSent());
        dto.put("components", template.components());
        dto.put("createdAt", template.createdAt());
        dto.put("updatedAt", template.updatedAt());

        if (template.metaTemplateId() != null) {
            dto.put("metaTemplateId", template.metaTemplateId());
        }

        if (template.rejectionReason() != null) {
            dto.put("rejectionReason", template.rejectionReason());
        }

        return dto;
    }
    
    /**
     * Parsea componentes desde el request JSON
     */
    @SuppressWarnings("unchecked")
    private List<TemplateComponent> parseComponents(List<Map<String, Object>> componentsData) {
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
}
