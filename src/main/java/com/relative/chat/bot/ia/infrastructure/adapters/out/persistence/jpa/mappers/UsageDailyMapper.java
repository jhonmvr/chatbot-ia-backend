package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.UsageDailyEntity;
import com.relative.chat.bot.ia.domain.identity.UsageDaily;

public final class UsageDailyMapper {
    
    private UsageDailyMapper() {}
    
    public static UsageDaily toDomain(UsageDailyEntity e) {
        if (e == null) return null;
        
        return new UsageDaily(
            MappingHelpers.toLongId(e.getId()),
            e.getClientEntity() != null ? MappingHelpers.toUuidId(e.getClientEntity().getId()) : null,
            e.getDay(),
            e.getMessagesIn() != null ? e.getMessagesIn() : 0L,
            e.getMessagesOut() != null ? e.getMessagesOut() : 0L,
            e.getTokensIn() != null ? e.getTokensIn() : 0L,
            e.getTokensOut() != null ? e.getTokensOut() : 0L
        );
    }
    
    public static UsageDailyEntity toEntity(UsageDaily d) {
        if (d == null) return null;
        
        UsageDailyEntity e = new UsageDailyEntity();
        
        // El ID se asigna solo si existe (update), null para insert
        if (d.id() != null && d.id().isPresent()) {
            e.setId(d.id().value());
        }
        
        e.setDay(d.day());
        e.setMessagesIn(d.messagesIn());
        e.setMessagesOut(d.messagesOut());
        e.setTokensIn(d.tokensIn());
        e.setTokensOut(d.tokensOut());
        
        return e;
    }
}
