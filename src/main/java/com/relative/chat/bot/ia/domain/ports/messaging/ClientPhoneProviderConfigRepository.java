package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.messaging.ClientPhoneProviderConfig;
import com.relative.chat.bot.ia.domain.messaging.ProviderConfig;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para ClientPhoneProviderConfig
 */
public interface ClientPhoneProviderConfigRepository {
    
    /**
     * Busca una configuración por ID
     */
    Optional<ClientPhoneProviderConfig> findById(UuidId<ClientPhoneProviderConfig> id);
    
    /**
     * Busca configuraciones por client_phone_id
     */
    List<ClientPhoneProviderConfig> findByClientPhoneId(UuidId<ClientPhone> clientPhoneId);
    
    /**
     * Busca configuración activa por client_phone_id
     */
    List<ClientPhoneProviderConfig> findActiveByClientPhoneId(UuidId<ClientPhone> clientPhoneId);
    
    /**
     * Busca configuración específica por client_phone_id y provider_config_id
     */
    Optional<ClientPhoneProviderConfig> findByClientPhoneIdAndProviderConfigId(
            UuidId<ClientPhone> clientPhoneId, 
            UuidId<ProviderConfig> providerConfigId
    );
    
    /**
     * Busca configuración activa por client_phone_id y tipo de proveedor
     */
    Optional<ClientPhoneProviderConfig> findActiveByClientPhoneIdAndProviderType(
            UuidId<ClientPhone> clientPhoneId, 
            String providerType
    );
    
    /**
     * Guarda una configuración
     */
    void save(ClientPhoneProviderConfig config);
    
    /**
     * Elimina una configuración
     */
    void delete(UuidId<ClientPhoneProviderConfig> id);
}
