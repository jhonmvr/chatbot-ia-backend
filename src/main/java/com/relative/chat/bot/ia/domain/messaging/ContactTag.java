package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;

import java.time.Instant;

/**
 * Asociación entre un Contact y un Tag.
 * Representa la relación many-to-many entre contactos y etiquetas.
 */
public record ContactTag(
        UuidId<Contact> contactId,
        UuidId<Tag> tagId,
        Instant createdAt
) {

    /**
     * Crea una nueva relación Contact-Tag.
     */
    public static ContactTag create(
            UuidId<Contact> contactId,
            UuidId<Tag> tagId
    ) {
        Instant now = Instant.now();
        return new ContactTag(contactId, tagId, now);
    }

    /**
     * Representa una relación existente en el sistema (por ejemplo, cargada desde la base de datos).
     */
    public static ContactTag existing(
            UuidId<Contact> contactId,
            UuidId<Tag> tagId,
            Instant createdAt
    ) {
        return new ContactTag(contactId, tagId, createdAt);
    }

    /**
     * Devuelve una copia del objeto con una nueva fecha de creación (útil para duplicar registros o testing).
     */
    public ContactTag withCreatedAt(Instant newCreatedAt) {
        return new ContactTag(contactId, tagId, newCreatedAt);
    }
}