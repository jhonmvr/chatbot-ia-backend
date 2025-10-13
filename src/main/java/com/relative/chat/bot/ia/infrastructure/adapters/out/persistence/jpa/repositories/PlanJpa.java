package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;


import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface PlanJpa extends JpaRepository<PlanEntity, UUID> {
}
