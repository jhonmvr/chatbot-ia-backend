package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.MessageTemplateEntity;


public interface MessageTemplateJpa extends JpaRepository<MessageTemplateEntity, UUID> {
}
