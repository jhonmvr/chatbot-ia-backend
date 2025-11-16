package com.relative.chat.bot.ia.domain.scheduling;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Cuenta de proveedor de calendario configurada para un cliente
 * Soporta Google Calendar y Microsoft Outlook (Microsoft 365)
 */
public record CalendarProviderAccount(
        UuidId<CalendarProviderAccount> id,
        UuidId<Client> clientId,
        CalendarProvider provider,
        String accountEmail,
        String accessToken,
        String refreshToken,
        Instant tokenExpiresAt,
        Map<String, Object> config,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    
    /**
     * Constructor para nueva cuenta
     */
    public CalendarProviderAccount {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(clientId, "clientId no puede ser null");
        Objects.requireNonNull(provider, "provider no puede ser null");
        requireNonBlank(accountEmail, "accountEmail es requerido");
        requireNonBlank(accessToken, "accessToken es requerido");
        
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken es requerido");
        }
        
        if (isActive == null) {
            isActive = true;
        }
        
        if (config == null) {
            config = Map.of();
        }
        
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
    
    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
    
    /**
     * Verifica si el token está expirado
     */
    public boolean isTokenExpired() {
        if (tokenExpiresAt == null) {
            return false; // Si no hay fecha de expiración, asumimos que no está expirado
        }
        return Instant.now().isAfter(tokenExpiresAt);
    }
    
    /**
     * Crea una copia con el token actualizado
     */
    public CalendarProviderAccount withTokens(String newAccessToken, String newRefreshToken, Instant newExpiresAt) {
        return new CalendarProviderAccount(
                id,
                clientId,
                provider,
                accountEmail,
                newAccessToken,
                newRefreshToken,
                newExpiresAt,
                config,
                isActive,
                createdAt,
                Instant.now() // updatedAt
        );
    }
    
    /**
     * Crea una copia con el estado activo/inactivo actualizado
     */
    public CalendarProviderAccount withActive(Boolean active) {
        return new CalendarProviderAccount(
                id,
                clientId,
                provider,
                accountEmail,
                accessToken,
                refreshToken,
                tokenExpiresAt,
                config,
                active,
                createdAt,
                Instant.now() // updatedAt
        );
    }
    
    /**
     * Crea una copia con la configuración actualizada
     */
    public CalendarProviderAccount withConfig(Map<String, Object> newConfig) {
        return new CalendarProviderAccount(
                id,
                clientId,
                provider,
                accountEmail,
                accessToken,
                refreshToken,
                tokenExpiresAt,
                newConfig != null ? Map.copyOf(newConfig) : Map.of(),
                isActive,
                createdAt,
                Instant.now() // updatedAt
        );
    }
}

