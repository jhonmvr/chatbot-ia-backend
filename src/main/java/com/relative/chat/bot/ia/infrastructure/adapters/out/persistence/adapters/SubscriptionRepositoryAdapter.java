package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Subscription;
import com.relative.chat.bot.ia.domain.ports.identity.SubscriptionRepository;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.SubscriptionEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.SubscriptionJpa;

import jakarta.persistence.EntityManager;

import jakarta.persistence.PersistenceContext;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;


import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class SubscriptionRepositoryAdapter implements SubscriptionRepository {

  private final SubscriptionJpa repo;

  @PersistenceContext private EntityManager em;


  private static Subscription toDomain(SubscriptionEntity e){
    Subscription d = new Subscription(
        UuidId.of(e.getId()),
        UuidId.of(e.getClientEntity().getId()),
        e.getPlanEntity().getCode(),
        e.getStartDate()
    );

    if (e.getCancelAt() != null) d.cancel(e.getCancelAt().toLocalDate());
    return d;
  }

  @Override 
  public Optional<Subscription> findById(UuidId<Subscription> id){
    return repo.findById(id.value()).map(SubscriptionRepositoryAdapter::toDomain);
  }

  @Override public void save(Subscription d){
    SubscriptionEntity e = new SubscriptionEntity();

    e.setId(d.id().value());

    e.setClientEntity(em.getReference(ClientEntity.class, d.clientId().value()));

    // NOTE: plan debe resolverse por código -> setPlanEntity(...)
    // TODO: Buscar PlanEntity por código y establecerlo
    // Por ahora, esto causará error si se intenta usar sin el plan
    
    e.setStartDate(d.startDate());

    e.setStatus(d.status().name());
    
    // Campos opcionales
    d.endDate().ifPresent(e::setEndDate);
    d.cancelAt().ifPresent(cancelAt -> e.setCancelAt(cancelAt.atStartOfDay().atOffset(java.time.ZoneOffset.UTC)));
    
    // Timestamps requeridos
    e.setCreatedAt(java.time.OffsetDateTime.now());
    e.setUpdatedAt(java.time.OffsetDateTime.now());

    repo.save(e);

  }
}
