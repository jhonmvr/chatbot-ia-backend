package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ProviderConfig;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ProviderConfigEntity;

/**
 * Mapper para ProviderConfig
 */
public class ProviderConfigMapper {
    
    /**
     * Convierte de entidad JPA a dominio
     */
    public static ProviderConfig toDomain(ProviderConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new ProviderConfig(
            UuidId.of(entity.getId()),
            entity.getProviderName(),
            entity.getProviderType(),
            entity.getDisplayName(),
            entity.getDescription(),
            entity.getApiBaseUrl(),
            entity.getApiVersion(),
            entity.getWebhookUrlTemplate(),
            entity.getIsActive(),
            entity.getIsDefault(),
            entity.getConfigSchema()
        );
    }
    
    /**
     * Convierte de dominio a entidad JPA
     */
    public static ProviderConfigEntity toEntity(ProviderConfig domain) {
        if (domain == null) {
            return null;
        }
        
        ProviderConfigEntity entity = new ProviderConfigEntity();
        entity.setId(domain.id().value());
        entity.setProviderName(domain.providerName());
        entity.setProviderType(domain.providerType());
        entity.setDisplayName(domain.displayName());
        entity.setDescription(domain.description());
        entity.setApiBaseUrl(domain.apiBaseUrl());
        entity.setApiVersion(domain.apiVersion());
        entity.setWebhookUrlTemplate(domain.webhookUrlTemplate());
        entity.setIsActive(domain.isActive());
        entity.setIsDefault(domain.isDefault());
        entity.setConfigSchema(domain.configSchema());
        entity.setCreatedAt(java.time.OffsetDateTime.now());
        entity.setUpdatedAt(java.time.OffsetDateTime.now());
        
        return entity;
    }
}
