package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Tag;
import com.relative.chat.bot.ia.domain.messaging.ContactTag;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactTagRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ContactTagJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter del repositorio de relaciones Contact-Tag.
 */
@Repository
@RequiredArgsConstructor
public class ContactTagRepositoryAdapter implements ContactTagRepository {

    private final ContactTagJpa contactTagJpa;

    @PersistenceContext
    private EntityManager em;

    /**
     * Crea la relación entre un contacto y una etiqueta.
     */
    @Override
    @Transactional
    public void create(ContactTag contactTag) {
        contactTagJpa.insertContactTag(
                contactTag.contactId().value(),
                contactTag.tagId().value()
        );
    }

    /**
     * Elimina la relación entre un contacto y una etiqueta.
     */
    @Override
    @Transactional
    public void delete(UuidId<Contact> contactId, UuidId<Tag> tagId) {
        contactTagJpa.deleteByContactIdAndTagId(
                contactId.value(),
                tagId.value()
        );
    }
}