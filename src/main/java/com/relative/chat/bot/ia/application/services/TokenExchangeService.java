package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;
import com.relative.chat.bot.ia.infrastructure.config.OAuth2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

/**
 * Servicio para intercambiar códigos de autorización OAuth2 por tokens de acceso
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenExchangeService {
    
    private final OAuth2Properties oauth2Properties;
    private final WebClient.Builder webClientBuilder;
    
    /**
     * Intercambia un código de autorización por tokens de acceso
     * 
     * @param code Código de autorización recibido del callback
     * @param provider Proveedor de calendario (GOOGLE o OUTLOOK)
     * @return Tokens OAuth2 (access_token, refresh_token, expires_in)
     */
    public OAuth2Tokens exchangeCodeForTokens(String code, CalendarProvider provider) {
        log.info("Intercambiando código por tokens para provider={}", provider);
        
        if (provider == CalendarProvider.GOOGLE) {
            return exchangeGoogleCode(code);
        } else if (provider == CalendarProvider.OUTLOOK) {
            return exchangeOutlookCode(code);
        } else {
            throw new IllegalArgumentException("Proveedor no soportado: " + provider);
        }
    }
    
    /**
     * Intercambia código de Google por tokens
     */
    private OAuth2Tokens exchangeGoogleCode(String code) {
        OAuth2Properties.Google google = oauth2Properties.getGoogle();
        
        WebClient webClient = webClientBuilder.build();
        
        try {
            GoogleTokenResponse response = webClient.post()
                    .uri(google.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("code", code)
                            .with("client_id", google.getClientId())
                            .with("client_secret", google.getClientSecret())
                            .with("redirect_uri", google.getRedirectUri())
                            .with("grant_type", "authorization_code"))
                    .retrieve()
                    .bodyToMono(GoogleTokenResponse.class)
                    .block();
            
            if (response == null || response.access_token() == null) {
                throw new RuntimeException("Error al intercambiar código por tokens de Google");
            }
            
            Instant expiresAt = response.expires_in() != null
                    ? Instant.now().plusSeconds(response.expires_in())
                    : null;
            
            log.info("Tokens de Google obtenidos exitosamente. Expira en: {}", expiresAt);
            
            return new OAuth2Tokens(
                    response.access_token(),
                    response.refresh_token(),
                    expiresAt
            );
            
        } catch (Exception e) {
            log.error("Error al intercambiar código de Google por tokens: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener tokens de Google: " + e.getMessage(), e);
        }
    }
    
    /**
     * Intercambia código de Outlook por tokens
     */
    private OAuth2Tokens exchangeOutlookCode(String code) {
        OAuth2Properties.Outlook outlook = oauth2Properties.getOutlook();
        
        WebClient webClient = webClientBuilder.build();
        
        try {
            OutlookTokenResponse response = webClient.post()
                    .uri(outlook.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("code", code)
                            .with("client_id", outlook.getClientId())
                            .with("client_secret", outlook.getClientSecret())
                            .with("redirect_uri", outlook.getRedirectUri())
                            .with("grant_type", "authorization_code")
                            .with("scope", String.join(" ", outlook.getScopes())))
                    .retrieve()
                    .bodyToMono(OutlookTokenResponse.class)
                    .block();
            
            if (response == null || response.access_token() == null) {
                throw new RuntimeException("Error al intercambiar código por tokens de Outlook");
            }
            
            Instant expiresAt = response.expires_in() != null
                    ? Instant.now().plusSeconds(response.expires_in())
                    : null;
            
            log.info("Tokens de Outlook obtenidos exitosamente. Expira en: {}", expiresAt);
            
            return new OAuth2Tokens(
                    response.access_token(),
                    response.refresh_token(),
                    expiresAt
            );
            
        } catch (Exception e) {
            log.error("Error al intercambiar código de Outlook por tokens: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener tokens de Outlook: " + e.getMessage(), e);
        }
    }
    
    /**
     * Renueva un access token usando el refresh token
     * 
     * @param refreshToken Refresh token
     * @param provider Proveedor de calendario
     * @return Nuevos tokens OAuth2
     */
    public OAuth2Tokens refreshTokens(String refreshToken, CalendarProvider provider) {
        log.info("Renovando tokens para provider={}", provider);
        
        if (provider == CalendarProvider.GOOGLE) {
            return refreshGoogleTokens(refreshToken);
        } else if (provider == CalendarProvider.OUTLOOK) {
            return refreshOutlookTokens(refreshToken);
        } else {
            throw new IllegalArgumentException("Proveedor no soportado: " + provider);
        }
    }
    
    /**
     * Renueva tokens de Google
     */
    private OAuth2Tokens refreshGoogleTokens(String refreshToken) {
        OAuth2Properties.Google google = oauth2Properties.getGoogle();
        
        WebClient webClient = webClientBuilder.build();
        
        try {
            GoogleTokenResponse response = webClient.post()
                    .uri(google.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("refresh_token", refreshToken)
                            .with("client_id", google.getClientId())
                            .with("client_secret", google.getClientSecret())
                            .with("grant_type", "refresh_token"))
                    .retrieve()
                    .bodyToMono(GoogleTokenResponse.class)
                    .block();
            
            if (response == null || response.access_token() == null) {
                throw new RuntimeException("Error al renovar tokens de Google");
            }
            
            Instant expiresAt = response.expires_in() != null
                    ? Instant.now().plusSeconds(response.expires_in())
                    : null;
            
            // Google puede o no devolver un nuevo refresh_token
            // Si no lo devuelve, mantener el anterior
            String newRefreshToken = response.refresh_token() != null
                    ? response.refresh_token()
                    : refreshToken;
            
            return new OAuth2Tokens(
                    response.access_token(),
                    newRefreshToken,
                    expiresAt
            );
            
        } catch (Exception e) {
            log.error("Error al renovar tokens de Google: {}", e.getMessage(), e);
            throw new RuntimeException("Error al renovar tokens de Google: " + e.getMessage(), e);
        }
    }
    
    /**
     * Renueva tokens de Outlook
     */
    private OAuth2Tokens refreshOutlookTokens(String refreshToken) {
        OAuth2Properties.Outlook outlook = oauth2Properties.getOutlook();
        
        WebClient webClient = webClientBuilder.build();
        
        try {
            OutlookTokenResponse response = webClient.post()
                    .uri(outlook.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("refresh_token", refreshToken)
                            .with("client_id", outlook.getClientId())
                            .with("client_secret", outlook.getClientSecret())
                            .with("grant_type", "refresh_token")
                            .with("scope", String.join(" ", outlook.getScopes())))
                    .retrieve()
                    .bodyToMono(OutlookTokenResponse.class)
                    .block();
            
            if (response == null || response.access_token() == null) {
                throw new RuntimeException("Error al renovar tokens de Outlook");
            }
            
            Instant expiresAt = response.expires_in() != null
                    ? Instant.now().plusSeconds(response.expires_in())
                    : null;
            
            // Outlook puede o no devolver un nuevo refresh_token
            String newRefreshToken = response.refresh_token() != null
                    ? response.refresh_token()
                    : refreshToken;
            
            return new OAuth2Tokens(
                    response.access_token(),
                    newRefreshToken,
                    expiresAt
            );
            
        } catch (Exception e) {
            log.error("Error al renovar tokens de Outlook: {}", e.getMessage(), e);
            throw new RuntimeException("Error al renovar tokens de Outlook: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tokens OAuth2 obtenidos
     */
    public record OAuth2Tokens(
            String accessToken,
            String refreshToken,
            Instant expiresAt
    ) {}
    
    /**
     * Respuesta de token de Google
     */
    private record GoogleTokenResponse(
            String access_token,
            String refresh_token,
            Integer expires_in,
            String token_type,
            String scope
    ) {}
    
    /**
     * Respuesta de token de Outlook
     */
    private record OutlookTokenResponse(
            String access_token,
            String refresh_token,
            Integer expires_in,
            String token_type,
            String scope
    ) {}
}

