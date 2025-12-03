package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactEntity;
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
    
    /**
     * Búsqueda avanzada de conversaciones con filtros
     * Busca en títulos de conversación, contenido de mensajes y nombres de contactos
     */
    @Query("""
        SELECT DISTINCT c FROM ConversationEntity c
        LEFT JOIN c.contactEntity contact
        LEFT JOIN MessageEntity m ON m.conversationEntity.id = c.id
        WHERE (:clientId IS NULL OR c.clientEntity.id = :clientId)
          AND (:contactId IS NULL OR c.contactEntity.id = :contactId)
          AND (:status IS NULL OR :status = '' OR c.status = :status)
          AND (:channel IS NULL OR :channel = '' OR c.channel = :channel)
          AND (:query IS NULL OR :query = '' OR 
               LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(contact.displayName, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(contact.firstName, '') || ' ' || COALESCE(contact.lastName, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(contact.phoneE164, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(contact.email, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(m.body, '')) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY c.startedAt DESC
        """)
    Page<ConversationEntity> searchConversations(
        @Param("clientId") UUID clientId,
        @Param("query") String query,
        @Param("contactId") UUID contactId,
        @Param("status") String status,
        @Param("channel") String channel,
        Pageable pageable
    );
    
    /**
     * Obtiene todas las conversaciones de un contacto específico
     * Ordenadas por fecha de inicio descendente (más recientes primero)
     */
    @Query("""
        SELECT c FROM ConversationEntity c
        WHERE c.contactEntity.id = :contactId
        ORDER BY c.startedAt DESC
        """)
    Page<ConversationEntity> findByContactId(
        @Param("contactId") UUID contactId,
        Pageable pageable
    );
    
    /**
     * Obtiene contactos únicos que tengan conversaciones, ordenados por el mensaje más reciente
     * Usa una subconsulta en el SELECT para incluir la fecha del último mensaje y poder ordenar
     */
    @Query("""
        SELECT DISTINCT contact,
               (SELECT MAX(m3.createdAt)
                FROM MessageEntity m3
                INNER JOIN ConversationEntity c3 ON c3.id = m3.conversationEntity.id
                WHERE c3.contactEntity.id = contact.id) as lastMessageDate
        FROM ContactEntity contact
        INNER JOIN ConversationEntity conv ON conv.contactEntity.id = contact.id
        INNER JOIN MessageEntity msg ON msg.conversationEntity.id = conv.id
        WHERE contact.clientEntity.id = :clientId
          AND (:query IS NULL OR :query = '' OR 
               LOWER(COALESCE(contact.displayName, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(contact.firstName, '') || ' ' || COALESCE(contact.lastName, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(contact.phoneE164, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(msg.body, '')) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY lastMessageDate DESC NULLS LAST
        """)
    List<Object[]> findContactsWithConversationsRaw(
        @Param("clientId") UUID clientId,
        @Param("query") String query,
        Pageable pageable
    );
    
    /**
     * Método helper que extrae solo los ContactEntity de los resultados y construye la Page
     */
    default org.springframework.data.domain.Page<ContactEntity> findContactsWithConversations(
        UUID clientId,
        String query,
        Pageable pageable
    ) {
        List<Object[]> results = findContactsWithConversationsRaw(clientId, query, pageable);
        List<ContactEntity> contacts = results.stream()
            .map(result -> (ContactEntity) result[0])
            .toList();
        
        // Para obtener el total, necesitamos hacer una query de conteo
        long total = countContactsWithConversations(clientId, query);
        
        return new org.springframework.data.domain.PageImpl<>(
            contacts,
            pageable,
            total
        );
    }
    
    /**
     * Cuenta el total de contactos con conversaciones (para paginación)
     */
    @Query("""
        SELECT COUNT(DISTINCT contact.id)
        FROM ContactEntity contact
        INNER JOIN ConversationEntity conv ON conv.contactEntity.id = contact.id
        INNER JOIN MessageEntity msg ON msg.conversationEntity.id = conv.id
        WHERE contact.clientEntity.id = :clientId
          AND (:query IS NULL OR :query = '' OR 
               LOWER(COALESCE(contact.displayName, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(contact.firstName, '') || ' ' || COALESCE(contact.lastName, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(contact.phoneE164, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(msg.body, '')) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    long countContactsWithConversations(
        @Param("clientId") UUID clientId,
        @Param("query") String query
    );
}
