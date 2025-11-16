package com.relative.chat.bot.ia.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO de respuesta para consultas de disponibilidad (Free/Busy)
 */
public record FreeBusyResponse(
        String calendarId,
        Instant startTime,
        Instant endTime,
        List<TimeSlot> busySlots,
        List<TimeSlot> freeSlots
) {
    /**
     * Slot de tiempo (ocupado o libre)
     */
    public record TimeSlot(
            Instant start,
            Instant end
    ) {}
}

