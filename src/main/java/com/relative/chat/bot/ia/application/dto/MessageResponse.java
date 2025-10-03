package com.relative.chat.bot.ia.application.dto;

import java.util.UUID;

/**
 * DTO para respuesta de mensaje procesado
 */
public record MessageResponse(
        UUID messageId,
        UUID conversationId,
        String response,
        boolean success,
        String errorMessage
) {
    public static MessageResponse success(UUID messageId, UUID conversationId, String response) {
        return new MessageResponse(messageId, conversationId, response, true, null);
    }
    
    public static MessageResponse error(String errorMessage) {
        return new MessageResponse(null, null, null, false, errorMessage);
    }
}

