package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.UUID;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.MessageEntity;


public interface MessageJpa extends JpaRepository<MessageEntity, UUID> {
  List<MessageEntity> findTop100ByConversationEntityIdOrderByCreatedAtDesc(UUID conversationId);

}
