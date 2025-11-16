package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.dto.*;
import com.relative.chat.bot.ia.application.usecases.ManageCalendarProviderAccount;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API REST para gestión de cuentas de proveedores de calendario
 * Permite configurar Google Calendar y Microsoft Outlook para agendamiento de citas
 */
@Slf4j
@RestController
@RequestMapping("/api/calendar-provider-accounts")
@RequiredArgsConstructor
@Tag(name = "Calendar Provider Accounts", description = "API para gestionar configuraciones de proveedores de calendario (Google Calendar y Outlook)")
public class CalendarProviderAccountController {
    
    private final ManageCalendarProviderAccount manageCalendarProviderAccount;
    
    /**
     * Crea una nueva cuenta de proveedor de calendario
     * POST /api/calendar-provider-accounts
     */
    @Operation(
        summary = "Crear cuenta de proveedor de calendario",
        description = "Crea una nueva configuración de cuenta de calendario (Google Calendar o Outlook) para un cliente. " +
                     "Requiere credenciales OAuth2 (access_token y refresh_token)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cuenta creada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "data": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "clientId": "123e4567-e89b-12d3-a456-426614174000",
                        "provider": "GOOGLE",
                        "accountEmail": "usuario@gmail.com",
                        "tokenExpiresAt": "2025-12-31T23:59:59Z",
                        "config": {
                          "calendar_id": "primary",
                          "timezone": "America/Guayaquil"
                        },
                        "isActive": true,
                        "isTokenExpired": false,
                        "createdAt": "2025-10-03T10:30:00Z",
                        "updatedAt": "2025-10-03T10:30:00Z"
                      },
                      "message": "Cuenta de calendario creada exitosamente"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos o ya existe una cuenta activa",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Ya existe una cuenta activa de Google Calendar para este cliente"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createAccount(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos de la cuenta de calendario a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientId": "123e4567-e89b-12d3-a456-426614174000",
                      "provider": "GOOGLE",
                      "accountEmail": "usuario@gmail.com",
                      "accessToken": "ya29.a0AfH6SMC...",
                      "refreshToken": "1//0g...",
                      "tokenExpiresAt": "2025-12-31T23:59:59Z",
                      "config": {
                        "calendar_id": "primary",
                        "timezone": "America/Guayaquil"
                      },
                      "isActive": true
                    }
                    """)
            )
        )
        @RequestBody Map<String, Object> request
    ) {
        try {
            CreateCalendarProviderAccountRequest createRequest = parseCreateRequest(request);
            CalendarProviderAccountResponse response = manageCalendarProviderAccount.create(createRequest);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", toMap(response),
                "message", "Cuenta de calendario creada exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al crear cuenta de calendario: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Actualiza una cuenta existente
     * PUT /api/calendar-provider-accounts/{id}
     */
    @Operation(
        summary = "Actualizar cuenta de proveedor de calendario",
        description = "Actualiza una cuenta de calendario existente. Permite actualizar tokens OAuth2, configuración y estado activo."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cuenta actualizada exitosamente"
        ),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateAccount(
        @Parameter(description = "ID de la cuenta de calendario", required = true)
        @PathVariable String id,
        @RequestBody Map<String, Object> request
    ) {
        try {
            UpdateCalendarProviderAccountRequest updateRequest = parseUpdateRequest(request);
            CalendarProviderAccountResponse response = manageCalendarProviderAccount.update(id, updateRequest);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", toMap(response),
                "message", "Cuenta de calendario actualizada exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al actualizar cuenta de calendario: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Obtiene una cuenta por ID
     * GET /api/calendar-provider-accounts/{id}
     */
    @Operation(
        summary = "Obtener cuenta de calendario por ID",
        description = "Retorna los detalles de una cuenta de calendario específica"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cuenta encontrada"
        ),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAccount(
        @Parameter(description = "ID de la cuenta de calendario", required = true)
        @PathVariable String id
    ) {
        Optional<CalendarProviderAccountResponse> accountOpt = manageCalendarProviderAccount.findById(id);
        
        if (accountOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data", toMap(accountOpt.get())
        ));
    }
    
    /**
     * Obtiene todas las cuentas de un cliente
     * GET /api/calendar-provider-accounts?clientId={clientId}
     */
    @Operation(
        summary = "Obtener cuentas de calendario de un cliente",
        description = "Retorna todas las cuentas de calendario configuradas para un cliente específico"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de cuentas obtenida correctamente"
        )
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAccountsByClient(
        @Parameter(description = "ID del cliente", required = true)
        @RequestParam String clientId,
        @Parameter(description = "Solo cuentas activas", required = false)
        @RequestParam(required = false, defaultValue = "false") boolean activeOnly
    ) {
        List<CalendarProviderAccountResponse> accounts = activeOnly
                ? manageCalendarProviderAccount.findActiveByClientId(clientId)
                : manageCalendarProviderAccount.findByClientId(clientId);
        
        List<Map<String, Object>> accountsList = accounts.stream()
                .map(this::toMap)
                .toList();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data", accountsList,
            "count", accountsList.size()
        ));
    }
    
    /**
     * Obtiene cuenta activa por cliente y proveedor
     * GET /api/calendar-provider-accounts/active?clientId={clientId}&provider={provider}
     */
    @Operation(
        summary = "Obtener cuenta activa por cliente y proveedor",
        description = "Retorna la cuenta activa de un proveedor específico para un cliente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cuenta encontrada"
        ),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveAccount(
        @Parameter(description = "ID del cliente", required = true)
        @RequestParam String clientId,
        @Parameter(description = "Proveedor de calendario (GOOGLE o OUTLOOK)", required = true)
        @RequestParam String provider
    ) {
        try {
            CalendarProvider calendarProvider = CalendarProvider.valueOf(provider.toUpperCase());
            Optional<CalendarProviderAccountResponse> accountOpt = 
                    manageCalendarProviderAccount.findActiveByClientIdAndProvider(clientId, calendarProvider);
            
            if (accountOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", toMap(accountOpt.get())
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Proveedor inválido. Valores permitidos: GOOGLE, OUTLOOK"
            ));
        }
    }
    
    /**
     * Elimina una cuenta
     * DELETE /api/calendar-provider-accounts/{id}
     */
    @Operation(
        summary = "Eliminar cuenta de calendario",
        description = "Elimina una cuenta de calendario del sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cuenta eliminada exitosamente"
        ),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAccount(
        @Parameter(description = "ID de la cuenta de calendario", required = true)
        @PathVariable String id
    ) {
        try {
            manageCalendarProviderAccount.delete(id);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cuenta de calendario eliminada exitosamente"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al eliminar cuenta de calendario: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private CreateCalendarProviderAccountRequest parseCreateRequest(Map<String, Object> request) {
        String clientId = (String) request.get("clientId");
        String providerStr = (String) request.get("provider");
        String accountEmail = (String) request.get("accountEmail");
        String accessToken = (String) request.get("accessToken");
        String refreshToken = (String) request.get("refreshToken");
        Boolean isActive = request.get("isActive") != null 
                ? Boolean.valueOf(request.get("isActive").toString()) 
                : true;
        
        CalendarProvider provider = providerStr != null 
                ? CalendarProvider.valueOf(providerStr.toUpperCase()) 
                : null;
        
        java.time.Instant tokenExpiresAt = null;
        if (request.get("tokenExpiresAt") != null) {
            String expiresAtStr = request.get("tokenExpiresAt").toString();
            tokenExpiresAt = java.time.Instant.parse(expiresAtStr);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) request.get("config");
        
        return new CreateCalendarProviderAccountRequest(
                clientId,
                provider,
                accountEmail,
                accessToken,
                refreshToken,
                tokenExpiresAt,
                config,
                isActive
        );
    }
    
    private UpdateCalendarProviderAccountRequest parseUpdateRequest(Map<String, Object> request) {
        String accessToken = (String) request.get("accessToken");
        String refreshToken = (String) request.get("refreshToken");
        Boolean isActive = request.get("isActive") != null 
                ? Boolean.valueOf(request.get("isActive").toString()) 
                : null;
        
        java.time.Instant tokenExpiresAt = null;
        if (request.get("tokenExpiresAt") != null) {
            String expiresAtStr = request.get("tokenExpiresAt").toString();
            tokenExpiresAt = java.time.Instant.parse(expiresAtStr);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) request.get("config");
        
        return new UpdateCalendarProviderAccountRequest(
                accessToken,
                refreshToken,
                tokenExpiresAt,
                config,
                isActive
        );
    }
    
    private Map<String, Object> toMap(CalendarProviderAccountResponse response) {
        return Map.of(
                "id", response.id(),
                "clientId", response.clientId(),
                "provider", response.provider().name(),
                "accountEmail", response.accountEmail(),
                "tokenExpiresAt", response.tokenExpiresAt() != null ? response.tokenExpiresAt().toString() : null,
                "config", response.config() != null ? response.config() : Map.of(),
                "isActive", response.isActive(),
                "isTokenExpired", response.isTokenExpired(),
                "createdAt", response.createdAt().toString(),
                "updatedAt", response.updatedAt().toString()
        );
    }
}

