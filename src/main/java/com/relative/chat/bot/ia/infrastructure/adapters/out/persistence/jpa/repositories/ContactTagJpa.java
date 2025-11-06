package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactTagEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface ContactTagJpa extends JpaRepository<ContactTagEntity, ContactTagId> {

    /**
     * Elimina la relación entre un contacto y una etiqueta.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM ContactTagEntity ct WHERE ct.id.contactId = :contactId AND ct.id.tagId = :tagId")
    void deleteByContactIdAndTagId(@Param("contactId") UUID contactId, @Param("tagId") UUID tagId);

    /**
     * Crea una relación contacto-etiqueta (INSERT).
     * En JPA normalmente se usa save(), pero se agrega por si se requiere SQL directo.
     */
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO chatbotia.contact_tag (contact_id, tag_id, created_at)
        VALUES (:contactId, :tagId, NOW())
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    void insertContactTag(@Param("contactId") UUID contactId, @Param("tagId") UUID tagId);
}