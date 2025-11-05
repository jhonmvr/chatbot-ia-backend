package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    
    /**
     * Búsqueda avanzada de contactos con múltiples filtros
     * Si query es nulo o vacío, busca en todos los contactos
     * Si clientId es nulo, busca en todos los clientes
     */
    @Query("""
        SELECT DISTINCT c FROM ContactEntity c 
        LEFT JOIN c.tags t
        WHERE (:clientId IS NULL OR c.clientEntity.id = :clientId)
          AND (:query IS NULL OR :query = '' OR 
               LOWER(c.displayName) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(c.firstName, '') || ' ' || COALESCE(c.lastName, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(c.phoneE164) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(c.externalId) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:isVip IS NULL OR c.isVip = :isVip)
          AND (:isActive IS NULL OR c.isActive = :isActive)
          AND (:tag IS NULL OR :tag = '' OR t.name = :tag)
        ORDER BY c.displayName ASC
        """)
    Page<ContactEntity> searchContacts(
        @Param("clientId") UUID clientId,
        @Param("query") String query,
        @Param("isVip") Boolean isVip,
        @Param("isActive") Boolean isActive,
        @Param("tag") String tag,
        Pageable pageable
    );
}
