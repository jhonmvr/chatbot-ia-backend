package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Tag;
import com.relative.chat.bot.ia.domain.messaging.ContactTag;

import java.util.List;

/**
 * Puerto de repositorio para manejar relaciones Contact-Tag.
 */
public interface ContactTagRepository {

    /**
     * Crea una nueva relación entre un contacto y una etiqueta.
     */
    void create(ContactTag contactTag);

    /**
     * Elimina la relación entre un contacto y una etiqueta.
     */
    void delete(UuidId<Contact> contactId, UuidId<Tag> tagId);


    boolean existTag(UuidId<Contact> contactId, UuidId<Tag> tagId);

    List<ContactTag> findAllByContactId(UuidId<Contact> contactId);
}
