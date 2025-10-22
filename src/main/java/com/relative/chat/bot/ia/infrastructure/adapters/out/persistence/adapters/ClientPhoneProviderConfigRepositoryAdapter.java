package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.messaging.ClientPhoneProviderConfig;
import com.relative.chat.bot.ia.domain.messaging.ProviderConfig;
import com.relative.chat.bot.ia.domain.ports.messaging.ClientPhoneProviderConfigRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneProviderConfigEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ProviderConfigEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers.ClientPhoneProviderConfigMapper;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ClientPhoneJpa;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ClientPhoneProviderConfigJpa;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ProviderConfigJpa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de repositorio para ClientPhoneProviderConfig
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ClientPhoneProviderConfigRepositoryAdapter implements ClientPhoneProviderConfigRepository {
    
    private final ClientPhoneProviderConfigJpa clientPhoneProviderConfigJpa;
    private final ClientPhoneJpa clientPhoneJpa;
    private final ProviderConfigJpa providerConfigJpa;
    
    @Override
    public Optional<ClientPhoneProviderConfig> findById(UuidId<ClientPhoneProviderConfig> id) {
        return clientPhoneProviderConfigJpa.findById(id.value())
                .map(ClientPhoneProviderConfigMapper::toDomain);
    }
    
    @Override
    public List<ClientPhoneProviderConfig> findByClientPhoneId(UuidId<ClientPhone> clientPhoneId) {
        return clientPhoneProviderConfigJpa.findByClientPhoneEntityId(clientPhoneId.value())
                .stream()
                .map(ClientPhoneProviderConfigMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ClientPhoneProviderConfig> findActiveByClientPhoneId(UuidId<ClientPhone> clientPhoneId) {
        return clientPhoneProviderConfigJpa.findByClientPhoneEntityIdAndIsActiveTrue(clientPhoneId.value())
                .stream()
                .map(ClientPhoneProviderConfigMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<ClientPhoneProviderConfig> findByClientPhoneIdAndProviderConfigId(
            UuidId<ClientPhone> clientPhoneId, 
            UuidId<ProviderConfig> providerConfigId
    ) {
        return clientPhoneProviderConfigJpa.findByClientPhoneEntityIdAndProviderConfigEntityId(
                clientPhoneId.value(), 
                providerConfigId.value()
        ).map(ClientPhoneProviderConfigMapper::toDomain);
    }
    
    @Override
    public Optional<ClientPhoneProviderConfig> findActiveByClientPhoneIdAndProviderType(
            UuidId<ClientPhone> clientPhoneId, 
            String providerType
    ) {
        return clientPhoneProviderConfigJpa.findByClientPhoneIdAndProviderType(
                clientPhoneId.value(), 
                providerType
        ).map(ClientPhoneProviderConfigMapper::toDomain);
    }
    
    @Override
    public void save(ClientPhoneProviderConfig config) {
        ClientPhoneEntity clientPhoneEntity = clientPhoneJpa.findById(config.clientPhoneId().value())
                .orElseThrow(() -> new IllegalArgumentException("ClientPhone no encontrado: " + config.clientPhoneId().value()));
        
        ProviderConfigEntity providerConfigEntity = providerConfigJpa.findById(config.providerConfigId().value())
                .orElseThrow(() -> new IllegalArgumentException("ProviderConfig no encontrado: " + config.providerConfigId().value()));
        
        ClientPhoneProviderConfigEntity entity = ClientPhoneProviderConfigMapper.toEntity(
                config, 
                clientPhoneEntity, 
                providerConfigEntity
        );
        
        clientPhoneProviderConfigJpa.save(entity);
    }
    
    @Override
    public void delete(UuidId<ClientPhoneProviderConfig> id) {
        clientPhoneProviderConfigJpa.deleteById(id.value());
    }
}
