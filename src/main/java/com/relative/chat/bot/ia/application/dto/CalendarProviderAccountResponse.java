package com.relative.chat.bot.ia.application.dto;

import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;

import java.time.Instant;
import java.util.Map;

/**
 * DTO de respuesta para CalendarProviderAccount
 */
public record CalendarProviderAccountResponse(
        String id,
        String clientId,
        CalendarProvider provider,
        String accountEmail,
        Instant tokenExpiresAt,
        Map<String, Object> config,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        Boolean isTokenExpired
) {}

