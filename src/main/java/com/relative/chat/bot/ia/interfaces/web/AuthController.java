package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.services.ApiKeyService;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller para autenticación de clientes
 * Endpoints para login y generación de tokens
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API para autenticación de clientes y generación de tokens")
public class AuthController {
    
    private final ApiKeyService apiKeyService;
    
    /**
     * Endpoint de login con credenciales API Key
     * POST /api/auth/login
     */
    @Operation(
        summary = "Autenticación con API Key",
        description = "Autentica un cliente usando su API Key y API Secret. Retorna un token que debe usarse en las peticiones subsiguientes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Autenticación exitosa",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "token": "550e8400-e29b-41d4-a716-446655440000-AbCdEf123456",
                      "clientId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "Autenticación exitosa"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Credenciales inválidas"
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
                      "message": "apiKey y apiSecret son requeridos"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Error en autenticación: <mensaje de error>"
                    }
                    """)
            )
        )
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> login(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Credenciales de autenticación",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "apiKey": "sk_AbCdEf1234567890",
                      "apiSecret": "secret_xyz1234567890abcdefghijklmnopqrstuvwxyz"
                    }
                    """)
            )
        )
        @RequestBody Map<String, String> request
    ) {
        try {
            String apiKey = request.get("apiKey");
            String apiSecret = request.get("apiSecret");
            
            if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "apiKey y apiSecret son requeridos"
                ));
            }
            
            Optional<ApiKeyService.TokenResult> tokenResultOpt = apiKeyService.authenticate(apiKey, apiSecret);
            
            if (tokenResultOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "message", "Credenciales inválidas"
                ));
            }
            
            ApiKeyService.TokenResult tokenResult = tokenResultOpt.get();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "token", tokenResult.token(),
                "clientId", tokenResult.clientId().value().toString(),
                "message", "Autenticación exitosa"
            ));
            
        } catch (Exception e) {
            log.error("Error en autenticación: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error en autenticación: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint para generar API Key inicial para un cliente
     * POST /api/auth/generate-api-key
     * 
     * IMPORTANTE: Este endpoint genera las credenciales iniciales (apiKey y apiSecret).
     * El apiSecret solo se muestra UNA VEZ al generarlo. Guárdelo de forma segura.
     */
    @Operation(
        summary = "Generar API Key para un cliente",
        description = "Genera una nueva API Key y API Secret para un cliente. " +
                "El apiSecret solo se muestra UNA VEZ al generarlo. " +
                "Si ya existe una API Key para el cliente, se reemplazará. " +
                "Guarde las credenciales de forma segura ya que no se pueden recuperar después."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API Key generada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "apiKey": "sk_AbCdEf1234567890GhIjKlMnOpQrStUvWxYz",
                      "apiSecret": "xyz1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456",
                      "clientId": "550e8400-e29b-41d4-a716-446655440000",
                      "message": "API Key generada exitosamente. Guarde el apiSecret de forma segura, solo se muestra una vez."
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
                      "message": "clientId es requerido"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente no encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Cliente no encontrado: 550e8400-e29b-41d4-a716-446655440000"
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Error al generar API Key: <mensaje de error>"
                    }
                    """)
            )
        )
    })
    @PostMapping(value = "/generate-api-key", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> generateApiKey(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "ID del cliente para generar la API Key",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "clientId": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    """)
            )
        )
        @RequestBody Map<String, String> request
    ) {
        try {
            String clientIdStr = request.get("clientId");
            
            if (clientIdStr == null || clientIdStr.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "clientId es requerido"
                ));
            }
            
            UuidId<Client> clientId = UuidId.of(UUID.fromString(clientIdStr));
            
            // Generar API Key
            ApiKeyService.ApiKeyCreationResult result = apiKeyService.createApiKey(clientId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "apiKey", result.apiKey().apiKey(),
                "apiSecret", result.apiSecret(), // Solo se muestra una vez
                "clientId", clientId.value().toString(),
                "message", "API Key generada exitosamente. Guarde el apiSecret de forma segura, solo se muestra una vez."
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al generar API Key: {}", e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error interno al generar API Key: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error al generar API Key: " + e.getMessage()
            ));
        }
    }
}

