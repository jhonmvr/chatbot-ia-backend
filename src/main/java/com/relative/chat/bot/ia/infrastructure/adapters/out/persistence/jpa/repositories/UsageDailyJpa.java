package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

import java.util.Optional;

import java.util.UUID;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.UsageDailyEntity;


public interface UsageDailyJpa extends JpaRepository<UsageDailyEntity, Long> {
  Optional<UsageDailyEntity> findByClientEntityIdAndDay(UUID clientId, LocalDate day);

}
