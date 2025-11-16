package com.relative.chat.bot.ia.application.dto;

import java.time.Instant;

/**
 * DTO para consultar disponibilidad (Free/Busy) en calendario
 */
public record FreeBusyQuery(
        Instant startTime,
        Instant endTime,
        String timeZone
) {
    public FreeBusyQuery {
        if (startTime == null) {
            throw new IllegalArgumentException("startTime es requerido");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("endTime es requerido");
        }
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new IllegalArgumentException("endTime debe ser posterior a startTime");
        }
        if (timeZone == null || timeZone.isBlank()) {
            timeZone = "America/Guayaquil";
        }
    }
}

