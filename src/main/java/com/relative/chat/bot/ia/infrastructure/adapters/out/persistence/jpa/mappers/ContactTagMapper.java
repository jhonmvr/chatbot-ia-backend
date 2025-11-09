package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.messaging.ContactTag;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactTagEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.TagEntity;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class ContactTagMapper {

    private ContactTagMapper() {}

    public static ContactTag toDomain(ContactTagEntity e) {
        if (e == null) return null;

        return ContactTag.existing(
                MappingHelpers.toUuidId(e.getId().getContactId()),
                MappingHelpers.toUuidId(e.getId().getTagId()),
                e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null
        );
    }

    public static ContactTagEntity toEntity(ContactTag d) {
        if (d == null) return null;

        ContactTagEntity e = new ContactTagEntity();
        var id = new com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactTagId(
                d.contactId().value(),
                d.tagId().value()
        );
        e.setId(id);
        e.setCreatedAt(d.createdAt() != null ? OffsetDateTime.ofInstant(d.createdAt(), ZoneOffset.UTC) : OffsetDateTime.now());
        return e;
    }

    /**
     * Versión con EntityManager, útil para relaciones ya gestionadas.
     */
    public static ContactTagEntity toEntity(ContactTag d, EntityManager em) {
        if (d == null) return null;

        ContactTagEntity e = toEntity(d);

        // Se asocian las entidades referenciadas sin cargar completamente desde base
        ContactEntity contact = em.getReference(ContactEntity.class, d.contactId().value());
        TagEntity tag = em.getReference(TagEntity.class, d.tagId().value());

        e.setContact(contact);
        e.setTag(tag);

        return e;
    }
}
