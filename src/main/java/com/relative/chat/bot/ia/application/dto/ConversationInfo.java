package com.relative.chat.bot.ia.application.dto;

import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.types.ConversationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para información de conversación
 */
public record ConversationInfo(
        UUID id,
        UUID clientId,
        UUID contactId,
        Channel channel,
        ConversationStatus status,
        String title,
        Instant startedAt,
        Instant closedAt,
        int messageCount
) {}

