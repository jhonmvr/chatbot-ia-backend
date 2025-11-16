package com.relative.chat.bot.ia.interfaces.web;

import com.relative.chat.bot.ia.application.dto.CreateCalendarProviderAccountRequest;
import com.relative.chat.bot.ia.application.dto.CalendarProviderAccountResponse;
import com.relative.chat.bot.ia.application.services.OAuth2Service;
import com.relative.chat.bot.ia.application.services.TokenExchangeService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * API REST para el flujo OAuth2 de proveedores de calendario
 * Maneja la autorización de Google Calendar y Microsoft Outlook
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth2/calendar")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Calendar", description = "API para autorización OAuth2 de proveedores de calendario")
public class OAuth2Controller {
    
    private final OAuth2Service oauth2Service;
    private final TokenExchangeService tokenExchangeService;
    private final ManageCalendarProviderAccount manageCalendarProviderAccount;
    
    /**
     * Genera URL de autorización OAuth2
     * GET /api/oauth2/calendar/authorize?clientId={id}&provider={GOOGLE|OUTLOOK}
     */
    @Operation(
        summary = "Generar URL de autorización OAuth2",
        description = "Genera una URL de autorización OAuth2 para que el usuario autorice el acceso a su calendario. " +
                     "El usuario debe ser redirigido a esta URL para completar la autorización."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "URL de autorización generada exitosamente",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "authorizationUrl": "https://accounts.google.com/o/oauth2/v2/auth?...",
                      "state": "abc123...",
                      "message": "Redirija al usuario a authorizationUrl para completar la autorización"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos")
    })
    @GetMapping("/authorize")
    public ResponseEntity<Map<String, Object>> generateAuthorizationUrl(
        @Parameter(description = "ID del cliente", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
        @RequestParam String clientId,
        @Parameter(description = "Proveedor de calendario (GOOGLE o OUTLOOK)", required = true, example = "GOOGLE")
        @RequestParam String provider
    ) {
        try {
            CalendarProvider calendarProvider = CalendarProvider.valueOf(provider.toUpperCase());
            OAuth2Service.OAuth2AuthorizationUrl authUrl = oauth2Service.generateAuthorizationUrl(calendarProvider, clientId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "authorizationUrl", authUrl.url(),
                "state", authUrl.state(),
                "message", "Redirija al usuario a authorizationUrl para completar la autorización"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Proveedor inválido. Valores permitidos: GOOGLE, OUTLOOK"
            ));
        } catch (Exception e) {
            log.error("Error al generar URL de autorización: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Callback OAuth2 - recibe el código de autorización
     * GET /api/oauth2/calendar/callback?code={code}&state={state}
     */
    @Operation(
        summary = "Callback OAuth2",
        description = "Endpoint de callback que recibe el código de autorización después de que el usuario autoriza el acceso. " +
                     "Intercambia el código por tokens y crea/actualiza la cuenta de calendario."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "302",
            description = "Redirección después de procesar el callback"
        ),
        @ApiResponse(responseCode = "400", description = "Código o state inválido")
    })
    @GetMapping("/callback")
    public RedirectView handleCallback(
        @Parameter(description = "Código de autorización OAuth2", required = true)
        @RequestParam(required = false) String code,
        @Parameter(description = "State token para validar la solicitud", required = true)
        @RequestParam String state,
        @Parameter(description = "Código de error si la autorización falló")
        @RequestParam(required = false) String error,
        @Parameter(description = "Descripción del error")
        @RequestParam(required = false) String error_description
    ) {
        try {
            // Validar state
            OAuth2Service.OAuth2State oauth2State = oauth2Service.validateAndGetState(state);
            String clientId = oauth2State.clientId();
            CalendarProvider provider = oauth2State.provider();
            
            // Si hay un error en la autorización
            if (error != null) {
                log.warn("Error en autorización OAuth2: {} - {}", error, error_description);
                return new RedirectView(String.format("/oauth2/calendar/error?error=%s&error_description=%s", 
                    error, error_description != null ? error_description : ""));
            }
            
            // Si no hay código, error
            if (code == null || code.isBlank()) {
                log.error("Código de autorización no recibido en callback");
                return new RedirectView("/oauth2/calendar/error?error=missing_code");
            }
            
            // Intercambiar código por tokens
            TokenExchangeService.OAuth2Tokens tokens = tokenExchangeService.exchangeCodeForTokens(code, provider);
            
            // Obtener información del usuario (email) desde el provider
            // Por ahora usamos un email genérico, pero en el siguiente paso obtendremos el email real
            String accountEmail = "user@example.com"; // TODO: Obtener email real del provider
            
            // Crear o actualizar la cuenta de calendario
            CreateCalendarProviderAccountRequest createRequest = new CreateCalendarProviderAccountRequest(
                    clientId,
                    provider,
                    accountEmail,
                    tokens.accessToken(),
                    tokens.refreshToken(),
                    tokens.expiresAt(),
                    Map.of(), // Config adicional vacía por ahora
                    true
            );
            
            CalendarProviderAccountResponse account;
            try {
                account = manageCalendarProviderAccount.create(createRequest);
                log.info("Cuenta de calendario creada exitosamente: id={}", account.id());
            return new RedirectView(String.format("/oauth2/calendar/success?accountId=%s", account.id()));
            } catch (IllegalArgumentException e) {
                // Si ya existe, intentar actualizar
                var existingOpt = manageCalendarProviderAccount.findActiveByClientIdAndProvider(clientId, provider);
                if (existingOpt.isPresent()) {
                    var updateRequest = new com.relative.chat.bot.ia.application.dto.UpdateCalendarProviderAccountRequest(
                            tokens.accessToken(),
                            tokens.refreshToken(),
                            tokens.expiresAt(),
                            null,
                            null
                    );
                    account = manageCalendarProviderAccount.update(existingOpt.get().id(), updateRequest);
                    log.info("Cuenta de calendario actualizada exitosamente: id={}", account.id());
                    return new RedirectView(String.format("/oauth2/calendar/success?accountId=%s", account.id()));
                }
                throw e;
            }
            
        } catch (Exception e) {
            log.error("Error al procesar callback OAuth2: {}", e.getMessage(), e);
            return new RedirectView(String.format("/oauth2/calendar/error?error=callback_error&error_description=%s", 
                e.getMessage()));
        }
    }
    
    /**
     * Página de éxito (puede ser una página HTML simple o redirección a frontend)
     * GET /api/oauth2/calendar/success?accountId={id}
     */
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> success(
        @RequestParam String accountId
    ) {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Autorización completada exitosamente",
            "accountId", accountId
        ));
    }
    
    /**
     * Página de error (puede ser una página HTML simple o redirección a frontend)
     * GET /api/oauth2/calendar/error?error={error}&error_description={description}
     */
    @GetMapping("/error")
    public ResponseEntity<Map<String, Object>> error(
        @RequestParam(required = false) String error,
        @RequestParam(required = false) String error_description
    ) {
        return ResponseEntity.badRequest().body(Map.of(
            "status", "error",
            "error", error != null ? error : "unknown_error",
            "error_description", error_description != null ? error_description : "Error desconocido"
        ));
    }
}

