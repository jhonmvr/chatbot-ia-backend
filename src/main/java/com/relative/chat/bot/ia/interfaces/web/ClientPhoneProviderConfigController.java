package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.messaging.ClientPhoneProviderConfig;
import com.relative.chat.bot.ia.domain.messaging.ProviderConfig;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.ClientPhoneProviderConfigRepository;
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
 * API REST para gestionar configuraciones específicas de proveedores para números de WhatsApp
 */
@Slf4j
@RestController
@RequestMapping("/api/client-phone-provider-configs")
@RequiredArgsConstructor
@Tag(name = "Client Phone Provider Configs", description = "API para gestionar configuraciones específicas de proveedores para números de WhatsApp")
public class ClientPhoneProviderConfigController {
    
    private final ClientPhoneProviderConfigRepository clientPhoneProviderConfigRepository;
    private final ClientPhoneRepository clientPhoneRepository;
    private final ProviderConfigRepository providerConfigRepository;
    
    /**
     * Crear una nueva configuración de proveedor para un número de WhatsApp
     * POST /api/client-phone-provider-configs
     */
    @Operation(
        summary = "Crear configuración de proveedor para un número de WhatsApp",
        description = "Asocia una configuración específica de proveedor (Meta, Twilio, etc.) a un número de WhatsApp existente."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Configuración creada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "configId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "Configuración de proveedor creada exitosamente"
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
                      "message": "clientPhoneId, providerConfigId y configValues son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "ClientPhone o ProviderConfig no encontrado")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createClientPhoneProviderConfig(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos de la configuración de proveedor a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientPhoneId": "a1234567-e89b-12d3-a456-426614174000",
                      "providerConfigId": "b1234567-e89b-12d3-a456-426614174001",
                      "configValues": {
                        "access_token": "EAAxxxxxxxxxxxx",
                        "phone_number_id": "123456789012345",
                        "api_version": "v21.0",
                        "verify_token": "mi_token_secreto"
                      }
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            // Validar campos requeridos
            String clientPhoneId = (String) request.get("clientPhoneId");
            String providerConfigId = (String) request.get("providerConfigId");
            @SuppressWarnings("unchecked")
            Map<String, Object> configValues = (Map<String, Object>) request.get("configValues");
            
            if (clientPhoneId == null || providerConfigId == null || configValues == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientPhoneId, providerConfigId y configValues son requeridos"
                ));
            }
            
            // Verificar que el ClientPhone existe
            Optional<ClientPhone> clientPhoneOpt = clientPhoneRepository.findById(UuidId.of(UUID.fromString(clientPhoneId)));
            if (clientPhoneOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "ClientPhone no encontrado: " + clientPhoneId
                ));
            }
            
            // Verificar que el ProviderConfig existe
            Optional<ProviderConfig> providerConfigOpt = providerConfigRepository.findById(UuidId.of(UUID.fromString(providerConfigId)));
            if (providerConfigOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "ProviderConfig no encontrado: " + providerConfigId
                ));
            }
            
            // Crear ClientPhoneProviderConfig
            ClientPhoneProviderConfig config = new ClientPhoneProviderConfig(
                UuidId.newId(),
                UuidId.of(UUID.fromString(clientPhoneId)),
                UuidId.of(UUID.fromString(providerConfigId)),
                configValues,
                true // isActive por defecto
            );
            
            // Guardar
            clientPhoneProviderConfigRepository.save(config);
            
            log.info("Configuración de proveedor creada: {} para ClientPhone {}", 
                    providerConfigOpt.get().providerType(), clientPhoneId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "configId", config.id().value().toString(),
                "message", "Configuración de proveedor creada exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al crear ClientPhoneProviderConfig: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al crear configuración: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtener configuraciones de proveedor para un número de WhatsApp
     * GET /api/client-phone-provider-configs/client-phone/{clientPhoneId}
     */
    @Operation(
        summary = "Listar configuraciones de proveedor para un número de WhatsApp",
        description = "Obtiene todas las configuraciones de proveedor asociadas a un número de WhatsApp específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuraciones obtenidas exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "configs": [
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "clientPhoneId": "a1234567-e89b-12d3-a456-426614174000",
                          "providerConfigId": "b1234567-e89b-12d3-a456-426614174001",
                          "providerType": "META",
                          "configValues": {
                            "access_token": "EAAxxxxxxxxxxxx",
                            "phone_number_id": "123456789012345",
                            "api_version": "v21.0"
                          }
                        }
                      ]
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "ClientPhone no encontrado")
    })
    @GetMapping("/client-phone/{clientPhoneId}")
    public ResponseEntity<Map<String, Object>> getConfigsByClientPhone(
        @Parameter(description = "UUID del número de WhatsApp", required = true)
        @PathVariable String clientPhoneId
    ) {
        try {
            // Verificar que el ClientPhone existe
            Optional<ClientPhone> clientPhoneOpt = clientPhoneRepository.findById(UuidId.of(UUID.fromString(clientPhoneId)));
            if (clientPhoneOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "ClientPhone no encontrado: " + clientPhoneId
                ));
            }
            
            // Obtener configuraciones
            List<ClientPhoneProviderConfig> configs = clientPhoneProviderConfigRepository.findByClientPhoneId(
                UuidId.of(UUID.fromString(clientPhoneId))
            );
            
            // Convertir a DTOs
            List<Map<String, Object>> configDtos = configs.stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "configs", configDtos
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener configuraciones: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener configuraciones: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtener configuración específica por ID
     * GET /api/client-phone-provider-configs/{id}
     */
    @Operation(
        summary = "Obtener configuración de proveedor por ID",
        description = "Obtiene una configuración específica de proveedor por su ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuración obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "config": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "clientPhoneId": "a1234567-e89b-12d3-a456-426614174000",
                        "providerConfigId": "b1234567-e89b-12d3-a456-426614174001",
                        "providerType": "META",
                        "configValues": {
                          "access_token": "EAAxxxxxxxxxxxx",
                          "phone_number_id": "123456789012345"
                        }
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Configuración no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getConfigById(
        @Parameter(description = "UUID de la configuración", required = true)
        @PathVariable String id
    ) {
        try {
            Optional<ClientPhoneProviderConfig> configOpt = clientPhoneProviderConfigRepository.findById(
                UuidId.of(UUID.fromString(id))
            );
            
            if (configOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Configuración no encontrada: " + id
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "config", toDto(configOpt.get())
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al obtener configuración: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al obtener configuración: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Actualizar configuración de proveedor
     * PUT /api/client-phone-provider-configs/{id}
     */
    @Operation(
        summary = "Actualizar configuración de proveedor",
        description = "Actualiza los valores de configuración de un proveedor específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuración actualizada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "configId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "Configuración actualizada exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Configuración no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateConfig(
        @Parameter(description = "UUID de la configuración a actualizar", required = true)
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Nuevos valores de configuración",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "configValues": {
                        "access_token": "EAAyyyyyyyyyyyy",
                        "phone_number_id": "987654321098765",
                        "api_version": "v22.0"
                      }
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            // Buscar configuración existente
            Optional<ClientPhoneProviderConfig> existingConfigOpt = clientPhoneProviderConfigRepository.findById(
                UuidId.of(UUID.fromString(id))
            );
            
            if (existingConfigOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Configuración no encontrada: " + id
                ));
            }
            
            ClientPhoneProviderConfig existingConfig = existingConfigOpt.get();
            
            // Obtener nuevos valores de configuración
            @SuppressWarnings("unchecked")
            Map<String, Object> newConfigValues = (Map<String, Object>) request.get("configValues");
            
            if (newConfigValues == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "configValues es requerido"
                ));
            }
            
            // Crear configuración actualizada
            ClientPhoneProviderConfig updatedConfig = new ClientPhoneProviderConfig(
                existingConfig.id(),
                existingConfig.clientPhoneId(),
                existingConfig.providerConfigId(),
                newConfigValues,
                existingConfig.isActive()
            );
            
            // Guardar
            clientPhoneProviderConfigRepository.save(updatedConfig);
            
            log.info("Configuración actualizada: {} para ClientPhone {}", 
                    id, existingConfig.clientPhoneId().value());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "configId", updatedConfig.id().value().toString(),
                "message", "Configuración actualizada exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al actualizar configuración: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al actualizar configuración: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Eliminar configuración de proveedor
     * DELETE /api/client-phone-provider-configs/{id}
     */
    @Operation(
        summary = "Eliminar configuración de proveedor",
        description = "Elimina una configuración específica de proveedor"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuración eliminada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "message": "Configuración eliminada exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Configuración no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfig(
        @Parameter(description = "UUID de la configuración a eliminar", required = true)
        @PathVariable String id
    ) {
        try {
            // Verificar que la configuración existe
            Optional<ClientPhoneProviderConfig> configOpt = clientPhoneProviderConfigRepository.findById(
                UuidId.of(UUID.fromString(id))
            );
            
            if (configOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Configuración no encontrada: " + id
                ));
            }
            
            // Eliminar
            clientPhoneProviderConfigRepository.delete(UuidId.of(UUID.fromString(id)));
            
            log.info("Configuración eliminada: {}", id);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Configuración eliminada exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al eliminar configuración: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al eliminar configuración: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Convierte un ClientPhoneProviderConfig del dominio a DTO
     */
    private Map<String, Object> toDto(ClientPhoneProviderConfig config) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", config.id().value().toString());
        dto.put("clientPhoneId", config.clientPhoneId().value().toString());
        dto.put("providerConfigId", config.providerConfigId().value().toString());
        dto.put("configValues", config.configValues());
        
        // Obtener información del proveedor
        Optional<ProviderConfig> providerConfigOpt = providerConfigRepository.findById(config.providerConfigId());
        if (providerConfigOpt.isPresent()) {
            ProviderConfig providerConfig = providerConfigOpt.get();
            dto.put("providerType", providerConfig.providerType());
            dto.put("providerName", providerConfig.providerName());
        }
        
        return dto;
    }
}
