package com.relative.chat.bot.ia.application.dto;

import java.time.Instant;
import java.util.Map;

/**
 * DTO para actualizar una cuenta de proveedor de calendario
 */
public record UpdateCalendarProviderAccountRequest(
        String accessToken,
        String refreshToken,
        Instant tokenExpiresAt,
        Map<String, Object> config,
        Boolean isActive
) {
    public UpdateCalendarProviderAccountRequest {
        // Todos los campos son opcionales para actualización
    }
}

