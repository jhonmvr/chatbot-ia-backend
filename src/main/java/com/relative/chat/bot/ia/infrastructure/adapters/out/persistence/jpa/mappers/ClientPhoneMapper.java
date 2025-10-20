package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.types.EntityStatus;
import com.relative.chat.bot.ia.domain.vo.PhoneE164;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneEntity;

/**
 * Mapper para ClientPhone
 */
public class ClientPhoneMapper {
    
    /**
     * Convierte de entidad JPA a dominio
     */
    public static ClientPhone toDomain(ClientPhoneEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new ClientPhone(
            UuidId.of(entity.getId()),
            UuidId.of(entity.getClientEntity().getId()),
            new PhoneE164(entity.getE164()),
            Channel.valueOf(entity.getChannel()),
            entity.getProvider(),
            entity.getProviderSid(),  // phone_number_id de Meta, etc.
            entity.getIsActive() ? EntityStatus.ACTIVE : EntityStatus.INACTIVE,
            entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : null,
            // Campos específicos para Meta WhatsApp
            entity.getMetaAccessToken(),
            entity.getMetaPhoneNumberId(),
            entity.getMetaApiVersion(),
            // Campos específicos para Twilio
            entity.getTwilioAccountSid(),
            entity.getTwilioAuthToken(),
            // Campos específicos para WWebJs
            entity.getWwebjsSessionId(),
            entity.getWwebjsWebhookUrl(),
            // Campos comunes para todos los proveedores
            entity.getApiBaseUrl(),
            entity.getWebhookUrl(),
            entity.getVerifyToken(),
            entity.getWebhookSecret()
        );
    }
    
    /**
     * Convierte de dominio a entidad JPA (para nuevas inserciones)
     */
    public static ClientPhoneEntity toEntity(ClientPhone domain, ClientEntity clientEntity) {
        ClientPhoneEntity entity = new ClientPhoneEntity();
        entity.setId(domain.id().value());
        entity.setClientEntity(clientEntity);
        entity.setE164(domain.phone().value());
        entity.setChannel(domain.channel().name());
        entity.setProvider(domain.provider());
        entity.setProviderSid(domain.providerSid());  // phone_number_id de Meta
        entity.setIsActive(domain.status() == EntityStatus.ACTIVE);
        entity.setIsDefault(false); // Por defecto no es el número predeterminado
        entity.setMetadata(new java.util.HashMap<>());
        entity.setCreatedAt(java.time.OffsetDateTime.now());
        entity.setUpdatedAt(java.time.OffsetDateTime.now());
        
        // Campos específicos para Meta WhatsApp
        entity.setMetaAccessToken(domain.metaAccessToken());
        entity.setMetaPhoneNumberId(domain.metaPhoneNumberId());
        entity.setMetaApiVersion(domain.metaApiVersion());
        
        // Campos específicos para Twilio
        entity.setTwilioAccountSid(domain.twilioAccountSid());
        entity.setTwilioAuthToken(domain.twilioAuthToken());
        
        // Campos específicos para WWebJs
        entity.setWwebjsSessionId(domain.wwebjsSessionId());
        entity.setWwebjsWebhookUrl(domain.wwebjsWebhookUrl());
        
        // Campos comunes para todos los proveedores
        entity.setApiBaseUrl(domain.apiBaseUrl());
        entity.setWebhookUrl(domain.webhookUrl());
        entity.setVerifyToken(domain.verifyToken());
        entity.setWebhookSecret(domain.webhookSecret());
        
        return entity;
    }
}
