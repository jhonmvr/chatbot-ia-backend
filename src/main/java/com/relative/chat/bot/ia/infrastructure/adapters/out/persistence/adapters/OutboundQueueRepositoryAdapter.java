package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.LongId;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.OutboundItem;
import com.relative.chat.bot.ia.domain.ports.messaging.OutboundQueueRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.types.QueueStatus;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.OutboundQueueEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.OutboundQueueJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboundQueueRepositoryAdapter implements OutboundQueueRepository {
    
    private final OutboundQueueJpa repo;
    
    @PersistenceContext
    private EntityManager em;
    
    private static OutboundItem toDomain(OutboundQueueEntity e) {
        return new OutboundItem(
            e.getId() != null ? LongId.of(e.getId()) : LongId.newId(),
            e.getClientEntity() != null ? UuidId.of(e.getClientEntity().getId()) : null,
            e.getContactEntity() != null ? UuidId.of(e.getContactEntity().getId()) : null,
            e.getConversationEntity() != null ? UuidId.of(e.getConversationEntity().getId()) : null,
            e.getPhone() != null ? UuidId.of(e.getPhone().getId()) : null,
            Channel.valueOf(e.getChannel()),
            e.getBody()
        );
    }
    
    @Override
    public void save(OutboundItem d) {
        OutboundQueueEntity e = new OutboundQueueEntity();
        
        // Solo setear ID si existe (update)
        if (d.id() != null && d.id().isPresent()) {
            e.setId(d.id().value());
        }
        
        // Relaciones (pueden ser null)
        if (d.clientId() != null) {
            e.setClientEntity(em.getReference(com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity.class, d.clientId().value()));
        }
        
        if (d.contactId().isPresent()) {
            e.setContactEntity(em.getReference(com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactEntity.class, d.contactId().get().value()));
        }
        
        if (d.conversationId().isPresent()) {
            e.setConversationEntity(em.getReference(com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ConversationEntity.class, d.conversationId().get().value()));
        }
        
        if (d.phoneId().isPresent()) {
            e.setPhone(em.getReference(com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneEntity.class, d.phoneId().get().value()));
        }
        
        e.setChannel(d.channel().name());
        e.setStatus(d.status().name());
        e.setBody(d.body());
        e.setRetries(d.retries());
        
        // Campos requeridos con defaults
        e.setMedia(new java.util.HashMap<>()); // default empty JSONB
        
        if (d.scheduleAt().isPresent()) {
            e.setScheduleAt(d.scheduleAt().get().atOffset(ZoneOffset.UTC));
        } else {
            e.setScheduleAt(OffsetDateTime.now());
        }
        
        if (d.lastError().isPresent()) {
            e.setLastError(d.lastError().get());
        }
        
        // Timestamps requeridos
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        
        repo.save(e);
    }
    
    @Override
    public List<OutboundItem> due(QueueStatus status, java.time.Instant now, int limit) {
        List<OutboundQueueEntity> rows = repo.findTop200ByStatusAndScheduleAtLessThanEqualOrderByScheduleAtAsc(
            com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.enums.QueueStatus.valueOf(status.name()),
            OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
        );
        
        return rows.stream()
            .limit(limit)
            .map(OutboundQueueRepositoryAdapter::toDomain)
            .toList();
    }
}
