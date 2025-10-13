package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.KbEntity;


public interface KbJpa extends JpaRepository<KbEntity, UUID> {
    List<KbEntity> findByClientEntityId(UUID clientId);
}
