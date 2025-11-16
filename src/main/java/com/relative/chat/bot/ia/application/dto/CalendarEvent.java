package com.relative.chat.bot.ia.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO para crear o actualizar un evento de calendario
 */
public record CalendarEvent(
        String summary,
        String description,
        Instant startTime,
        Instant endTime,
        String timeZone,
        String location,
        List<String> attendeeEmails,
        Boolean isOnlineMeeting,
        String onlineMeetingProvider
) {
    public CalendarEvent {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary es requerido");
        }
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
            timeZone = "America/Guayaquil"; // Default timezone
        }
        if (attendeeEmails == null) {
            attendeeEmails = List.of();
        }
        if (isOnlineMeeting == null) {
            isOnlineMeeting = false;
        }
    }
}

