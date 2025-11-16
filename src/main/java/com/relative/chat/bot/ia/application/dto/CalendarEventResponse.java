package com.relative.chat.bot.ia.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO de respuesta para eventos de calendario
 */
public record CalendarEventResponse(
        String eventId,
        String summary,
        String description,
        Instant startTime,
        Instant endTime,
        String timeZone,
        String location,
        List<String> attendeeEmails,
        String organizerEmail,
        String status,
        String htmlLink,
        Boolean isOnlineMeeting,
        String onlineMeetingUrl,
        Instant createdAt,
        Instant updatedAt
) {}

