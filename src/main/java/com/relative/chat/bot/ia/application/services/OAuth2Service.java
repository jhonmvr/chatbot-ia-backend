package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;
import com.relative.chat.bot.ia.infrastructure.config.OAuth2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para gestionar el flujo OAuth2 de Google Calendar y Microsoft Outlook
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {
    
    private final OAuth2Properties oauth2Properties;
    
    /**
     * Almacena temporalmente los estados OAuth2 y el clientId asociado
     * En producción, debería usar Redis o una base de datos
     */
    private final Map<String, OAuth2State> oauth2States = new ConcurrentHashMap<>();
    
    private static final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Genera una URL de autorización OAuth2
     * 
     * @param provider Proveedor de calendario (GOOGLE o OUTLOOK)
     * @param clientId ID del cliente que está autorizando
     * @return URL de autorización y state token
     */
    public OAuth2AuthorizationUrl generateAuthorizationUrl(CalendarProvider provider, String clientId) {
        String state = generateState();
        
        // Guardar el state con el clientId asociado
        oauth2States.put(state, new OAuth2State(clientId, provider));
        
        String authorizationUrl;
        
        if (provider == CalendarProvider.GOOGLE) {
            authorizationUrl = buildGoogleAuthorizationUrl(state);
        } else if (provider == CalendarProvider.OUTLOOK) {
            authorizationUrl = buildOutlookAuthorizationUrl(state);
        } else {
            throw new IllegalArgumentException("Proveedor no soportado: " + provider);
        }
        
        log.info("URL de autorización generada para provider={}, clientId={}, state={}", 
                provider, clientId, state);
        
        return new OAuth2AuthorizationUrl(authorizationUrl, state);
    }
    
    /**
     * Construye la URL de autorización de Google
     */
    private String buildGoogleAuthorizationUrl(String state) {
        OAuth2Properties.Google google = oauth2Properties.getGoogle();
        
        return UriComponentsBuilder.fromHttpUrl(google.getAuthorizationUrl())
                .queryParam("client_id", google.getClientId())
                .queryParam("redirect_uri", google.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", google.getScopes()))
                .queryParam("access_type", "offline") // Necesario para obtener refresh_token
                .queryParam("prompt", "consent") // Fuerza mostrar consentimiento para obtener refresh_token
                .queryParam("state", state)
                .toUriString();
    }
    
    /**
     * Construye la URL de autorización de Microsoft Outlook
     */
    private String buildOutlookAuthorizationUrl(String state) {
        OAuth2Properties.Outlook outlook = oauth2Properties.getOutlook();
        
        return UriComponentsBuilder.fromHttpUrl(outlook.getAuthorizationUrl())
                .queryParam("client_id", outlook.getClientId())
                .queryParam("redirect_uri", outlook.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", outlook.getScopes()))
                .queryParam("response_mode", "query")
                .queryParam("state", state)
                .toUriString();
    }
    
    /**
     * Valida y obtiene el state OAuth2
     */
    public OAuth2State validateAndGetState(String state) {
        OAuth2State oauth2State = oauth2States.remove(state);
        if (oauth2State == null) {
            throw new IllegalArgumentException("State inválido o expirado: " + state);
        }
        return oauth2State;
    }
    
    /**
     * Genera un state token aleatorio y seguro
     */
    private String generateState() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * URL de autorización y state token
     */
    public record OAuth2AuthorizationUrl(String url, String state) {}
    
    /**
     * Estado OAuth2 almacenado temporalmente
     */
    public record OAuth2State(String clientId, CalendarProvider provider) {}
}

