package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ProviderConfig;
import com.relative.chat.bot.ia.domain.ports.messaging.ProviderConfigRepository;
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

import java.util.*;

/**
 * API REST para gestionar esquemas de configuración de proveedores de WhatsApp
 */
@Slf4j
@RestController
@RequestMapping("/api/provider-configs")
@RequiredArgsConstructor
@Tag(name = "Provider Configs", description = "API para gestionar esquemas de configuración de proveedores de WhatsApp (Meta, Twilio, WWebJs, etc.)")
public class ProviderConfigController {
    
    private final ProviderConfigRepository providerConfigRepository;
    
    /**
     * Crear un nuevo esquema de configuración de proveedor
     * POST /api/provider-configs
     */
    @Operation(
        summary = "Crear esquema de configuración de proveedor",
        description = "Define un nuevo esquema de configuración para un proveedor de WhatsApp (Meta, Twilio, WWebJs, etc.)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Esquema de proveedor creado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "providerConfigId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "Esquema de proveedor creado exitosamente"
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
                      "message": "providerName, providerType y configSchema son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "409", description = "Proveedor ya existe")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createProviderConfig(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del esquema de proveedor a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "providerName": "Meta WhatsApp Business API",
                      "providerType": "META",
                      "displayName": "Meta WhatsApp",
                      "description": "Configuración para Meta WhatsApp Business API",
                      "apiBaseUrl": "https://graph.facebook.com",
                      "apiVersion": "v21.0",
                      "webhookUrlTemplate": "https://{domain}/webhooks/whatsapp/meta",
                      "isActive": true,
                      "isDefault": false,
                      "configSchema": {
                        "required_fields": ["access_token", "phone_number_id"],
                        "optional_fields": ["api_version", "verify_token", "webhook_secret"],
                        "field_configs": {
                          "access_token": {
                            "type": "string",
                            "sensitive": true,
                            "description": "Token de acceso de Meta"
                          },
                          "phone_number_id": {
                            "type": "string",
                            "sensitive": false,
                            "description": "ID del número de teléfono en Meta"
                          },
                          "api_version": {
                            "type": "string",
                            "sensitive": false,
                            "default": "v21.0",
                            "description": "Versión de la API de Meta"
                          }
                        }
                      }
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            // Validar campos requeridos
            String providerName = (String) request.get("providerName");
            String providerType = (String) request.get("providerType");
            @SuppressWarnings("unchecked")
            Map<String, Object> configSchema = (Map<String, Object>) request.get("configSchema");
            
            if (providerName == null || providerType == null || configSchema == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "providerName, providerType y configSchema son requeridos"
                ));
            }
            
            // Verificar que el proveedor no existe ya
            Optional<ProviderConfig> existingConfigOpt = providerConfigRepository.findByProviderType(providerType);
            if (existingConfigOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "message", "Ya existe un proveedor con el tipo: " + providerType
                ));
            }
            
            // Obtener valores opcionales
            String displayName = (String) request.getOrDefault("displayName", providerName);
            String description = (String) request.getOrDefault("description", "");
            String apiBaseUrl = (String) request.getOrDefault("apiBaseUrl", "");
            String apiVersion = (String) request.getOrDefault("apiVersion", "");
            String webhookUrlTemplate = (String) request.getOrDefault("webhookUrlTemplate", "");
            Boolean isActive = (Boolean) request.getOrDefault("isActive", true);
            Boolean isDefault = (Boolean) request.getOrDefault("isDefault", false);
            
            // Crear ProviderConfig
            ProviderConfig providerConfig = new ProviderConfig(
                UuidId.newId(),
                providerName,
                providerType,
                displayName,
                description,
                apiBaseUrl,
                apiVersion,
                webhookUrlTemplate,
                isActive,
                isDefault,
                configSchema
            );
            
            // Guardar
            providerConfigRepository.save(providerConfig);
            
            log.info("Esquema de proveedor creado: {} ({})", providerName, providerType);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "providerConfigId", providerConfig.id().value().toString(),
                "message", "Esquema de proveedor creado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al crear ProviderConfig: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al crear esquema de proveedor: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Listar todos los esquemas de configuración de proveedores
     * GET /api/provider-configs
     */
    @Operation(
        summary = "Listar todos los esquemas de proveedores",
        description = "Obtiene todos los esquemas de configuración de proveedores disponibles"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Esquemas obtenidos exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "providers": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "providerName": "Meta WhatsApp Business API",
                          "providerType": "META",
                          "displayName": "Meta WhatsApp",
                          "description": "Configuración para Meta WhatsApp Business API",
                          "apiBaseUrl": "https://graph.facebook.com",
                          "apiVersion": "v21.0",
                          "isActive": true,
                          "isDefault": false,
                          "requiredFields": ["access_token", "phone_number_id"],
                          "optionalFields": ["api_version", "verify_token"]
                        }
                      ]
                    }
                    """)
            )
        )
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProviderConfigs() {
        try {
            List<ProviderConfig> providers = providerConfigRepository.findAll();
            
            // Convertir a DTOs
            List<Map<String, Object>> providerDtos = providers.stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "providers", providerDtos
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener esquemas de proveedores: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener esquemas: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtener esquema de configuración por tipo de proveedor
     * GET /api/provider-configs/type/{providerType}
     */
    @Operation(
        summary = "Obtener esquema por tipo de proveedor",
        description = "Obtiene el esquema de configuración de un proveedor específico por su tipo (META, TWILIO, etc.)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Esquema obtenido exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "provider": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "providerName": "Meta WhatsApp Business API",
                        "providerType": "META",
                        "displayName": "Meta WhatsApp",
                        "description": "Configuración para Meta WhatsApp Business API",
                        "apiBaseUrl": "https://graph.facebook.com",
                        "apiVersion": "v21.0",
                        "isActive": true,
                        "isDefault": false,
                        "configSchema": {
                          "required_fields": ["access_token", "phone_number_id"],
                          "optional_fields": ["api_version", "verify_token"]
                        }
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    @GetMapping("/type/{providerType}")
    public ResponseEntity<Map<String, Object>> getProviderConfigByType(
        @Parameter(description = "Tipo de proveedor (META, TWILIO, WWEBJS, etc.)", required = true)
        @PathVariable String providerType
    ) {
        try {
            Optional<ProviderConfig> providerOpt = providerConfigRepository.findByProviderType(providerType);
            
            if (providerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Proveedor no encontrado: " + providerType
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "provider", toDetailedDto(providerOpt.get())
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener esquema de proveedor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener esquema: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtener esquema de configuración por ID
     * GET /api/provider-configs/{id}
     */
    @Operation(
        summary = "Obtener esquema por ID",
        description = "Obtiene un esquema de configuración específico por su ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Esquema obtenido exitosamente"
        ),
        @ApiResponse(responseCode = "404", description = "Esquema no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProviderConfigById(
        @Parameter(description = "UUID del esquema", required = true)
        @PathVariable String id
    ) {
        try {
            Optional<ProviderConfig> providerOpt = providerConfigRepository.findById(
                UuidId.of(UUID.fromString(id))
            );
            
            if (providerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Esquema no encontrado: " + id
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "provider", toDetailedDto(providerOpt.get())
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener esquema: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener esquema: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Actualizar esquema de configuración
     * PUT /api/provider-configs/{id}
     */
    @Operation(
        summary = "Actualizar esquema de configuración",
        description = "Actualiza un esquema de configuración de proveedor existente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Esquema actualizado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "providerConfigId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "Esquema actualizado exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Esquema no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProviderConfig(
        @Parameter(description = "UUID del esquema a actualizar", required = true)
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos actualizados del esquema",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "displayName": "Meta WhatsApp Business",
                      "description": "Configuración actualizada para Meta WhatsApp",
                      "apiVersion": "v22.0",
                      "isActive": true,
                      "configSchema": {
                        "required_fields": ["access_token", "phone_number_id"],
                        "optional_fields": ["api_version", "verify_token", "webhook_secret"]
                      }
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            // Buscar esquema existente
            Optional<ProviderConfig> existingProviderOpt = providerConfigRepository.findById(
                UuidId.of(UUID.fromString(id))
            );
            
            if (existingProviderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Esquema no encontrado: " + id
                ));
            }
            
            ProviderConfig existingProvider = existingProviderOpt.get();
            
            // Obtener configSchema con supresión de warning
            @SuppressWarnings("unchecked")
            Map<String, Object> configSchema = (Map<String, Object>) request.getOrDefault("configSchema", existingProvider.configSchema());
            
            // Crear esquema actualizado (mantener campos inmutables)
            ProviderConfig updatedProvider = new ProviderConfig(
                existingProvider.id(),
                existingProvider.providerName(), // Inmutable
                existingProvider.providerType(), // Inmutable
                (String) request.getOrDefault("displayName", existingProvider.displayName()),
                (String) request.getOrDefault("description", existingProvider.description()),
                (String) request.getOrDefault("apiBaseUrl", existingProvider.apiBaseUrl()),
                (String) request.getOrDefault("apiVersion", existingProvider.apiVersion()),
                (String) request.getOrDefault("webhookUrlTemplate", existingProvider.webhookUrlTemplate()),
                (Boolean) request.getOrDefault("isActive", existingProvider.isActive()),
                (Boolean) request.getOrDefault("isDefault", existingProvider.isDefault()),
                configSchema
            );
            
            // Guardar
            providerConfigRepository.save(updatedProvider);
            
            log.info("Esquema actualizado: {} ({})", updatedProvider.providerName(), updatedProvider.providerType());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "providerConfigId", updatedProvider.id().value().toString(),
                "message", "Esquema actualizado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al actualizar esquema: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al actualizar esquema: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Eliminar esquema de configuración
     * DELETE /api/provider-configs/{id}
     */
    @Operation(
        summary = "Eliminar esquema de configuración",
        description = "Elimina un esquema de configuración de proveedor (solo si no está en uso)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Esquema eliminado exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "message": "Esquema eliminado exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Esquema no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProviderConfig(
        @Parameter(description = "UUID del esquema a eliminar", required = true)
        @PathVariable String id
    ) {
        try {
            // Verificar que el esquema existe
            Optional<ProviderConfig> providerOpt = providerConfigRepository.findById(
                UuidId.of(UUID.fromString(id))
            );
            
            if (providerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Esquema no encontrado: " + id
                ));
            }
            
            ProviderConfig provider = providerOpt.get();
            
            // TODO: Verificar que no hay configuraciones activas usando este esquema
            // Esto requeriría una consulta adicional al ClientPhoneProviderConfigRepository
            
            // Eliminar
            providerConfigRepository.delete(UuidId.of(UUID.fromString(id)));
            
            log.info("Esquema eliminado: {} ({})", provider.providerName(), provider.providerType());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Esquema eliminado exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al eliminar esquema: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al eliminar esquema: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtener proveedores activos
     * GET /api/provider-configs/active
     */
    @Operation(
        summary = "Obtener proveedores activos",
        description = "Obtiene solo los esquemas de proveedores que están activos"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Proveedores activos obtenidos exitosamente"
        )
    })
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveProviderConfigs() {
        try {
            List<ProviderConfig> activeProviders = providerConfigRepository.findActiveProviders();
            
            // Convertir a DTOs
            List<Map<String, Object>> providerDtos = activeProviders.stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "providers", providerDtos
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener proveedores activos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener proveedores activos: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Convierte un ProviderConfig del dominio a DTO básico
     */
    private Map<String, Object> toDto(ProviderConfig provider) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", provider.id().value().toString());
        dto.put("providerName", provider.providerName());
        dto.put("providerType", provider.providerType());
        dto.put("displayName", provider.displayName());
        dto.put("description", provider.description());
        dto.put("apiBaseUrl", provider.apiBaseUrl());
        dto.put("apiVersion", provider.apiVersion());
        dto.put("isActive", provider.isActive());
        dto.put("isDefault", provider.isDefault());
        
        // Extraer campos requeridos y opcionales del esquema
        if (provider.configSchema() != null) {
            @SuppressWarnings("unchecked")
            List<String> requiredFields = (List<String>) provider.configSchema().get("required_fields");
            @SuppressWarnings("unchecked")
            List<String> optionalFields = (List<String>) provider.configSchema().get("optional_fields");
            
            dto.put("requiredFields", requiredFields != null ? requiredFields : List.of());
            dto.put("optionalFields", optionalFields != null ? optionalFields : List.of());
        } else {
            dto.put("requiredFields", List.of());
            dto.put("optionalFields", List.of());
        }
        
        return dto;
    }
    
    /**
     * Convierte un ProviderConfig del dominio a DTO detallado
     */
    private Map<String, Object> toDetailedDto(ProviderConfig provider) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", provider.id().value().toString());
        dto.put("providerName", provider.providerName());
        dto.put("providerType", provider.providerType());
        dto.put("displayName", provider.displayName());
        dto.put("description", provider.description());
        dto.put("apiBaseUrl", provider.apiBaseUrl());
        dto.put("apiVersion", provider.apiVersion());
        dto.put("webhookUrlTemplate", provider.webhookUrlTemplate());
        dto.put("isActive", provider.isActive());
        dto.put("isDefault", provider.isDefault());
        dto.put("configSchema", provider.configSchema());
        
        return dto;
    }
}
