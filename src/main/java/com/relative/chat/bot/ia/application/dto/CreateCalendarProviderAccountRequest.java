package com.relative.chat.bot.ia.application.dto;

import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;

import java.time.Instant;
import java.util.Map;

/**
 * DTO para crear una cuenta de proveedor de calendario
 */
public record CreateCalendarProviderAccountRequest(
        String clientId,
        CalendarProvider provider,
        String accountEmail,
        String accessToken,
        String refreshToken,
        Instant tokenExpiresAt,
        Map<String, Object> config,
        Boolean isActive
) {
    public CreateCalendarProviderAccountRequest {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId es requerido");
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider es requerido");
        }
        if (accountEmail == null || accountEmail.isBlank()) {
            throw new IllegalArgumentException("accountEmail es requerido");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken es requerido");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken es requerido");
        }
        if (isActive == null) {
            isActive = true;
        }
        if (config == null) {
            config = Map.of();
        }
    }
}

