package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.UUID;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.KbDocumentEntity;


public interface KbDocumentJpa extends JpaRepository<KbDocumentEntity, UUID> {
  List<KbDocumentEntity> findByKbEntityId(UUID kbId);

}
