package com.relative.chat.bot.ia.application.dto;

import com.relative.chat.bot.ia.domain.types.Channel;

import java.time.Instant;

/**
 * DTO para recibir mensajes entrantes
 */
public record MessageCommand(
        String clientCode,
        String phoneNumber,
        String contactPhone,
        String contactName,
        Channel channel,
        String content,
        Instant receivedAt,
        String externalId
) {
    public MessageCommand {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("El contenido del mensaje no puede estar vacío");
        }
        if (channel == null) {
            channel = Channel.WHATSAPP;
        }
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}

