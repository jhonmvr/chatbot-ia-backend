package com.relative.chat.bot.ia.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración OAuth2 para Google Calendar y Microsoft Outlook
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2Properties {
    
    private Google google = new Google();
    private Outlook outlook = new Outlook();
    
    @Getter
    @Setter
    public static class Google {
        /**
         * Client ID de Google OAuth2
         * Obtener desde: https://console.cloud.google.com/apis/credentials
         */
        private String clientId;
        
        /**
         * Client Secret de Google OAuth2
         */
        private String clientSecret;
        
        /**
         * URL de redirección después de la autorización
         * Debe estar registrada en Google Cloud Console
         */
        private String redirectUri;
        
        /**
         * Scopes OAuth2 requeridos para Google Calendar
         * https://www.googleapis.com/auth/calendar - Acceso completo al calendario
         * https://www.googleapis.com/auth/calendar.events - Solo eventos
         */
        private String[] scopes = {
            "https://www.googleapis.com/auth/calendar",
            "https://www.googleapis.com/auth/calendar.events"
        };
        
        /**
         * URL de autorización de Google
         */
        private String authorizationUrl = "https://accounts.google.com/o/oauth2/v2/auth";
        
        /**
         * URL para intercambiar código por tokens
         */
        private String tokenUrl = "https://oauth2.googleapis.com/token";
    }
    
    @Getter
    @Setter
    public static class Outlook {
        /**
         * Client ID (Application ID) de Microsoft Azure AD
         * Obtener desde: https://portal.azure.com -> Azure Active Directory -> App registrations
         */
        private String clientId;
        
        /**
         * Client Secret (Value) de Microsoft Azure AD
         */
        private String clientSecret;
        
        /**
         * Tenant ID de Microsoft Azure AD
         * Puede ser "common", "organizations", "consumers" o un tenant específico
         */
        private String tenantId = "common";
        
        /**
         * URL de redirección después de la autorización
         * Debe estar registrada en Azure AD
         */
        private String redirectUri;
        
        /**
         * Scopes OAuth2 requeridos para Microsoft Graph Calendar
         * Calendars.ReadWrite - Permite leer y escribir calendarios
         * offline_access - Para obtener refresh tokens
         */
        private String[] scopes = {
            "Calendars.ReadWrite",
            "offline_access"
        };
        
        /**
         * URL base de autorización de Microsoft
         */
        private String authorizationBaseUrl = "https://login.microsoftonline.com";
        
        /**
         * URL de autorización de Microsoft (se construye con tenantId)
         */
        public String getAuthorizationUrl() {
            return String.format("%s/%s/oauth2/v2.0/authorize", authorizationBaseUrl, tenantId);
        }
        
        /**
         * URL para intercambiar código por tokens
         */
        public String getTokenUrl() {
            return String.format("%s/%s/oauth2/v2.0/token", authorizationBaseUrl, tenantId);
        }
    }
}

