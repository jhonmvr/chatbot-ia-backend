package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ClientPhoneProviderConfig;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneProviderConfigEntity;

/**
 * Mapper para ClientPhoneProviderConfig
 */
public class ClientPhoneProviderConfigMapper {
    
    /**
     * Convierte de entidad JPA a dominio
     */
    public static ClientPhoneProviderConfig toDomain(ClientPhoneProviderConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new ClientPhoneProviderConfig(
            UuidId.of(entity.getId()),
            UuidId.of(entity.getClientPhoneEntity().getId()),
            UuidId.of(entity.getProviderConfigEntity().getId()),
            entity.getConfigValues(),
            entity.getIsActive()
        );
    }
    
    /**
     * Convierte de dominio a entidad JPA
     */
    public static ClientPhoneProviderConfigEntity toEntity(
            ClientPhoneProviderConfig domain,
            com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneEntity clientPhoneEntity,
            com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ProviderConfigEntity providerConfigEntity
    ) {
        if (domain == null) {
            return null;
        }
        
        ClientPhoneProviderConfigEntity entity = new ClientPhoneProviderConfigEntity();
        entity.setId(domain.id().value());
        entity.setClientPhoneEntity(clientPhoneEntity);
        entity.setProviderConfigEntity(providerConfigEntity);
        entity.setConfigValues(domain.configValues());
        entity.setIsActive(domain.isActive());
        entity.setCreatedAt(java.time.OffsetDateTime.now());
        entity.setUpdatedAt(java.time.OffsetDateTime.now());
        
        return entity;
    }
}
