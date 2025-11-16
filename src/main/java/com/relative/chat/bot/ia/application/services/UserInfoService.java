package com.relative.chat.bot.ia.application.services;

import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Servicio para obtener información del usuario después de la autorización OAuth2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoService {
    
    private final WebClient.Builder webClientBuilder;
    
    /**
     * Obtiene el email del usuario desde el proveedor de calendario
     * 
     * @param accessToken Token de acceso OAuth2
     * @param provider Proveedor de calendario
     * @return Email del usuario
     */
    public String getUserEmail(String accessToken, CalendarProvider provider) {
        log.info("Obteniendo email del usuario para provider={}", provider);
        
        if (provider == CalendarProvider.GOOGLE) {
            return getGoogleUserEmail(accessToken);
        } else if (provider == CalendarProvider.OUTLOOK) {
            return getOutlookUserEmail(accessToken);
        } else {
            throw new IllegalArgumentException("Proveedor no soportado: " + provider);
        }
    }
    
    /**
     * Obtiene el email del usuario desde Google
     */
    private String getGoogleUserEmail(String accessToken) {
        WebClient webClient = webClientBuilder.build();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("email")) {
                throw new RuntimeException("No se pudo obtener el email del usuario de Google");
            }
            
            String email = (String) response.get("email");
            log.info("Email obtenido de Google: {}", email);
            return email;
            
        } catch (Exception e) {
            log.error("Error al obtener email de Google: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener email de Google: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene el email del usuario desde Outlook/Microsoft Graph
     */
    private String getOutlookUserEmail(String accessToken) {
        WebClient webClient = webClientBuilder.build();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri("https://graph.microsoft.com/v1.0/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("mail")) {
                // Intentar con userPrincipalName si mail no está disponible
                if (response != null && response.containsKey("userPrincipalName")) {
                    String email = (String) response.get("userPrincipalName");
                    log.info("Email obtenido de Outlook (userPrincipalName): {}", email);
                    return email;
                }
                throw new RuntimeException("No se pudo obtener el email del usuario de Outlook");
            }
            
            String email = (String) response.get("mail");
            log.info("Email obtenido de Outlook: {}", email);
            return email;
            
        } catch (Exception e) {
            log.error("Error al obtener email de Outlook: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener email de Outlook: " + e.getMessage(), e);
        }
    }
}

