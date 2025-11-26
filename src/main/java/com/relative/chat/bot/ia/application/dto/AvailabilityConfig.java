package com.relative.chat.bot.ia.application.dto;

import java.util.List;
import java.util.Map;

/**
 * Configuración de disponibilidad para agendamiento
 */
public record AvailabilityConfig(
    boolean enabled,
    int slotDurationMinutes,
    int advanceBookingDays,
    Map<String, DaySchedule> workingHours,
    List<Holiday> holidays,
    List<BlockedSlot> blockedSlots
) {
    public record DaySchedule(
        boolean enabled,
        String startTime,
        String endTime,
        List<Break> breaks
    ) {}
    
    public record Break(
        String startTime,
        String endTime,
        String description
    ) {}
    
    public record Holiday(
        String date,
        String description
    ) {}
    
    public record BlockedSlot(
        String date,
        String startTime,
        String endTime,
        String description
    ) {}
}

