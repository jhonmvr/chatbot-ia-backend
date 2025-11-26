package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;


import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ConversationEntity;


public interface ConversationJpa extends JpaRepository<ConversationEntity, UUID> {
    
    /**
     * Busca conversaciones abiertas por cliente, contacto y canal
     * Ordena por started_at DESC para obtener la más reciente primero
     */
    @Query("""
        SELECT c FROM ConversationEntity c
        WHERE c.clientEntity.id = :clientId
          AND c.contactEntity.id = :contactId
          AND c.channel = :channel
          AND c.status = 'OPEN'
        ORDER BY c.startedAt DESC
        """)
    List<ConversationEntity> findOpenByClientAndContactAndChannel(
            @Param("clientId") UUID clientId,
            @Param("contactId") UUID contactId,
            @Param("channel") String channel,
            Pageable pageable
    );
    
    /**
     * Busca la primera conversación abierta (más reciente)
     */
    default Optional<ConversationEntity> findFirstOpenByClientAndContactAndChannel(
            UUID clientId,
            UUID contactId,
            String channel
    ) {
        List<ConversationEntity> results = findOpenByClientAndContactAndChannel(
                clientId, contactId, channel, Pageable.ofSize(1)
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    /**
     * Busca todas las conversaciones abiertas
     */
    @Query("SELECT c FROM ConversationEntity c WHERE c.status = 'OPEN'")
    List<ConversationEntity> findAllOpen();
    
    /**
     * Busca conversaciones abiertas que no tienen mensajes desde una fecha específica
     * Usa una subconsulta para verificar el último mensaje de cada conversación
     */
    @Query("""
        SELECT DISTINCT c FROM ConversationEntity c
        WHERE c.status = 'OPEN'
          AND c.id NOT IN (
              SELECT DISTINCT m.conversationEntity.id
              FROM MessageEntity m
              WHERE m.createdAt > :since
          )
          AND (c.startedAt < :since OR c.startedAt IS NULL)
        """)
    List<ConversationEntity> findOpenInactiveSince(@Param("since") java.time.OffsetDateTime since);
}
