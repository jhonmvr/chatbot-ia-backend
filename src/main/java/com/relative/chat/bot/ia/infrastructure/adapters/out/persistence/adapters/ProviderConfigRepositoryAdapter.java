package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ProviderConfig;
import com.relative.chat.bot.ia.domain.ports.messaging.ProviderConfigRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ProviderConfigEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers.ProviderConfigMapper;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ProviderConfigJpa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de repositorio para ProviderConfig
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProviderConfigRepositoryAdapter implements ProviderConfigRepository {
    
    private final ProviderConfigJpa providerConfigJpa;
    
    @Override
    public Optional<ProviderConfig> findById(UuidId<ProviderConfig> id) {
        return providerConfigJpa.findById(id.value())
                .map(ProviderConfigMapper::toDomain);
    }
    
    @Override
    public Optional<ProviderConfig> findByProviderType(String providerType) {
        return providerConfigJpa.findByProviderType(providerType)
                .map(ProviderConfigMapper::toDomain);
    }
    
    @Override
    public List<ProviderConfig> findActiveProviders() {
        return providerConfigJpa.findByIsActiveTrue()
                .stream()
                .map(ProviderConfigMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<ProviderConfig> findDefaultProvider() {
        return providerConfigJpa.findByIsDefaultTrue()
                .map(ProviderConfigMapper::toDomain);
    }
    
    @Override
    public void save(ProviderConfig providerConfig) {
        ProviderConfigEntity entity = ProviderConfigMapper.toEntity(providerConfig);
        providerConfigJpa.save(entity);
    }
    
    @Override
    public List<ProviderConfig> findAll() {
        return providerConfigJpa.findAll()
                .stream()
                .map(ProviderConfigMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void delete(UuidId<ProviderConfig> id) {
        providerConfigJpa.deleteById(id.value());
    }
}
