package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactJpa extends JpaRepository<ContactEntity, UUID> {
    
    /**
     * Busca un contacto por cliente y número de teléfono
     */
    Optional<ContactEntity> findByClientEntityIdAndPhoneE164(UUID clientId, String phoneE164);
    
    /**
     * Busca todos los contactos de un cliente
     */
    List<ContactEntity> findByClientEntityId(UUID clientId);
}
