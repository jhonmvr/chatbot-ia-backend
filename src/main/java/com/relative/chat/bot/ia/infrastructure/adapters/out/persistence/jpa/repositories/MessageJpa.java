package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import java.util.UUID;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.MessageEntity;


public interface MessageJpa extends JpaRepository<MessageEntity, UUID> {
  List<MessageEntity> findTop100ByConversationEntityIdOrderByCreatedAtDesc(UUID conversationId);
  
  /**
   * Obtiene el último mensaje de un contacto (el más reciente de todas sus conversaciones)
   */
  @Query("""
      SELECT m FROM MessageEntity m
      INNER JOIN ConversationEntity c ON c.id = m.conversationEntity.id
      WHERE c.contactEntity.id = :contactId
      ORDER BY m.createdAt DESC
      """)
  List<MessageEntity> findLastMessageByContactId(@Param("contactId") UUID contactId, org.springframework.data.domain.Pageable pageable);
  
  /**
   * Obtiene el último mensaje de un contacto (método helper)
   */
  default Optional<MessageEntity> findLastMessageByContact(UUID contactId) {
    List<MessageEntity> results = findLastMessageByContactId(contactId, org.springframework.data.domain.PageRequest.of(0, 1));
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
  }
}
