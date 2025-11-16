package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.CalendarProviderAccountEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Mapper para CalendarProviderAccount
 */
public class CalendarProviderAccountMapper {
    
    /**
     * Convierte de entidad JPA a dominio
     */
    public static CalendarProviderAccount toDomain(CalendarProviderAccountEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new CalendarProviderAccount(
            UuidId.of(entity.getId()),
            UuidId.of(entity.getClientEntity().getId()),
            CalendarProvider.valueOf(entity.getProvider()),
            entity.getAccountEmail(),
            entity.getAccessToken(),
            entity.getRefreshToken(),
            entity.getTokenExpiresAt() != null ? entity.getTokenExpiresAt() : null,
            entity.getConfig() != null ? Map.copyOf(entity.getConfig()) : Map.of(),
            entity.getIsActive(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : Instant.now(),
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().toInstant() : Instant.now()
        );
    }
    
    /**
     * Convierte de dominio a entidad JPA
     */
    public static CalendarProviderAccountEntity toEntity(
            CalendarProviderAccount domain,
            ClientEntity clientEntity
    ) {
        if (domain == null) {
            return null;
        }
        
        CalendarProviderAccountEntity entity = new CalendarProviderAccountEntity();
        entity.setId(domain.id().value());
        entity.setClientEntity(clientEntity);
        entity.setProvider(domain.provider().name());
        entity.setAccountEmail(domain.accountEmail());
        entity.setAccessToken(domain.accessToken());
        entity.setRefreshToken(domain.refreshToken());
        entity.setTokenExpiresAt(domain.tokenExpiresAt());
        entity.setConfig(domain.config() != null ? Map.copyOf(domain.config()) : Map.of());
        entity.setIsActive(domain.isActive());
        
        OffsetDateTime now = OffsetDateTime.now();
        if (domain.createdAt() != null) {
            entity.setCreatedAt(domain.createdAt().atOffset(java.time.ZoneOffset.UTC));
        } else {
            entity.setCreatedAt(now);
        }
        
        if (domain.updatedAt() != null) {
            entity.setUpdatedAt(domain.updatedAt().atOffset(java.time.ZoneOffset.UTC));
        } else {
            entity.setUpdatedAt(now);
        }
        
        return entity;
    }
}

