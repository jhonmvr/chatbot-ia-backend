package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.ApiKey;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.ports.identity.ApiKeyRepository;
import com.relative.chat.bot.ia.domain.types.EntityStatus;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ApiKeyEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ApiKeyJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ApiKeyRepositoryAdapter implements ApiKeyRepository {
    
    private final ApiKeyJpa repo;
    
    private static ApiKey toDomain(ApiKeyEntity e) {
        return ApiKey.existing(
                UuidId.of(e.getId()),
                UuidId.of(e.getClientId()),
                e.getApiKey(),
                e.getApiSecretHash(),
                EntityStatus.valueOf(e.getStatus()),
                e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : Instant.now(),
                e.getLastUsedAt() != null ? e.getLastUsedAt().toInstant() : null
        );
    }
    
    private static ApiKeyEntity toEntity(ApiKey d) {
        ApiKeyEntity e = new ApiKeyEntity();
        e.setId(d.id().value());
        e.setClientId(d.clientId().value());
        e.setApiKey(d.apiKey());
        e.setApiSecretHash(d.apiSecretHash());
        e.setStatus(d.status().name());
        e.setCreatedAt(d.createdAt() != null ? 
                java.time.OffsetDateTime.ofInstant(d.createdAt(), java.time.ZoneId.systemDefault()) : 
                java.time.OffsetDateTime.now());
        e.setLastUsedAt(d.lastUsedAt() != null ? 
                java.time.OffsetDateTime.ofInstant(d.lastUsedAt(), java.time.ZoneId.systemDefault()) : 
                null);
        return e;
    }
    
    @Override
    public Optional<ApiKey> findById(UuidId<ApiKey> id) {
        return repo.findById(id.value()).map(ApiKeyRepositoryAdapter::toDomain);
    }
    
    @Override
    public Optional<ApiKey> findByApiKey(String apiKey) {
        return repo.findByApiKey(apiKey).map(ApiKeyRepositoryAdapter::toDomain);
    }
    
    @Override
    public List<ApiKey> findByClientId(UuidId<Client> clientId) {
        return repo.findByClientId(clientId.value())
                .stream()
                .map(ApiKeyRepositoryAdapter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ApiKey> findActiveByClientId(UuidId<Client> clientId) {
        return repo.findByClientIdAndStatus(clientId.value(), EntityStatus.ACTIVE.name())
                .stream()
                .map(ApiKeyRepositoryAdapter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void save(ApiKey apiKey) {
        ApiKeyEntity entity = toEntity(apiKey);
        repo.save(entity);
    }
    
    @Override
    public void delete(UuidId<ApiKey> id) {
        repo.deleteById(id.value());
    }
    
    @Override
    public boolean existsByApiKey(String apiKey) {
        return repo.existsByApiKey(apiKey);
    }
}

