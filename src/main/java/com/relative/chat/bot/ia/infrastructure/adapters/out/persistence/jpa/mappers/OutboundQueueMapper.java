package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.OutboundQueueEntity;
import com.relative.chat.bot.ia.domain.messaging.OutboundItem;
import com.relative.chat.bot.ia.domain.types.Channel;

import java.time.ZoneOffset;

public final class OutboundQueueMapper {
    
    private OutboundQueueMapper() {}
    
    public static OutboundItem toDomain(OutboundQueueEntity e) {
        if (e == null) return null;
        
        return new OutboundItem(
            MappingHelpers.toLongId(e.getId()),
            e.getClientEntity() != null ? MappingHelpers.toUuidId(e.getClientEntity().getId()) : null,
            e.getContactEntity() != null ? MappingHelpers.toUuidId(e.getContactEntity().getId()) : null,
            e.getConversationEntity() != null ? MappingHelpers.toUuidId(e.getConversationEntity().getId()) : null,
            e.getPhone() != null ? MappingHelpers.toUuidId(e.getPhone().getId()) : null,
            e.getChannel() == null ? Channel.WHATSAPP : Channel.valueOf(e.getChannel()),
            e.getBody()
        );
    }
    
    public static OutboundQueueEntity toEntity(OutboundItem d) {
        if (d == null) return null;
        
        OutboundQueueEntity e = new OutboundQueueEntity();
        
        // El ID se asigna solo si existe (update), null para insert
        if (d.id() != null && d.id().isPresent()) {
            e.setId(d.id().value());
        }
        
        e.setChannel(d.channel().name());
        e.setStatus(d.status().name());
        e.setBody(d.body());
        e.setRetries(d.retries());
        
        d.scheduleAt().ifPresent(ts -> e.setScheduleAt(ts.atOffset(ZoneOffset.UTC)));
        d.lastError().ifPresent(e::setLastError);
        
        return e;
    }
}
