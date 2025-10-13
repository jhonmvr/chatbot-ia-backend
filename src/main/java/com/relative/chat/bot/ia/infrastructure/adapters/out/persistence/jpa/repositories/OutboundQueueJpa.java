package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

import java.util.List;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.OutboundQueueEntity;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.enums.QueueStatus;


public interface OutboundQueueJpa extends JpaRepository<OutboundQueueEntity, Long> {
  List<OutboundQueueEntity> findTop200ByStatusAndScheduleAtLessThanEqualOrderByScheduleAtAsc(QueueStatus status, OffsetDateTime now);

}
