package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.ports.scheduling.CalendarProviderAccountRepository;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.CalendarProviderAccountEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers.CalendarProviderAccountMapper;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.CalendarProviderAccountJpa;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ClientJpa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de repositorio para CalendarProviderAccount
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CalendarProviderAccountRepositoryAdapter implements CalendarProviderAccountRepository {
    
    private final CalendarProviderAccountJpa calendarProviderAccountJpa;
    private final ClientJpa clientJpa;
    
    @Override
    public Optional<CalendarProviderAccount> findById(UuidId<CalendarProviderAccount> id) {
        return calendarProviderAccountJpa.findById(id.value())
                .map(CalendarProviderAccountMapper::toDomain);
    }
    
    @Override
    public List<CalendarProviderAccount> findByClientId(UuidId<Client> clientId) {
        return calendarProviderAccountJpa.findByClientEntityId(clientId.value())
                .stream()
                .map(CalendarProviderAccountMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CalendarProviderAccount> findActiveByClientId(UuidId<Client> clientId) {
        return calendarProviderAccountJpa.findByClientEntityIdAndIsActiveTrue(clientId.value())
                .stream()
                .map(CalendarProviderAccountMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<CalendarProviderAccount> findActiveByClientIdAndProvider(UuidId<Client> clientId, CalendarProvider provider) {
        return calendarProviderAccountJpa.findByClientEntityIdAndProviderAndIsActiveTrue(clientId.value(), provider.name())
                .map(CalendarProviderAccountMapper::toDomain);
    }
    
    @Override
    public Optional<CalendarProviderAccount> findByClientIdAndProvider(UuidId<Client> clientId, CalendarProvider provider) {
        return calendarProviderAccountJpa.findByClientEntityIdAndProvider(clientId.value(), provider.name())
                .map(CalendarProviderAccountMapper::toDomain);
    }
    
    @Override
    public Optional<CalendarProviderAccount> findByAccountEmailAndProvider(String accountEmail, CalendarProvider provider) {
        return calendarProviderAccountJpa.findByAccountEmailAndProvider(accountEmail, provider.name())
                .map(CalendarProviderAccountMapper::toDomain);
    }
    
    @Override
    public void save(CalendarProviderAccount account) {
        ClientEntity clientEntity = clientJpa.findById(account.clientId().value())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + account.clientId().value()));
        
        CalendarProviderAccountEntity entity = calendarProviderAccountJpa.findById(account.id().value())
                .orElseGet(() -> {
                    CalendarProviderAccountEntity newEntity = CalendarProviderAccountMapper.toEntity(account, clientEntity);
                    newEntity.setId(account.id().value());
                    return newEntity;
                });
        
        // Actualizar campos
        entity.setClientEntity(clientEntity);
        entity.setProvider(account.provider().name());
        entity.setAccountEmail(account.accountEmail());
        entity.setAccessToken(account.accessToken());
        entity.setRefreshToken(account.refreshToken());
        entity.setTokenExpiresAt(account.tokenExpiresAt());
        entity.setConfig(account.config() != null ? account.config() : java.util.Map.of());
        entity.setIsActive(account.isActive());
        entity.setUpdatedAt(java.time.OffsetDateTime.now());
        
        calendarProviderAccountJpa.save(entity);
    }
    
    @Override
    public void delete(UuidId<CalendarProviderAccount> id) {
        calendarProviderAccountJpa.deleteById(id.value());
    }
    
    @Override
    public boolean existsActiveByClientIdAndProvider(UuidId<Client> clientId, CalendarProvider provider) {
        return calendarProviderAccountJpa.existsByClientEntityIdAndProviderAndIsActiveTrue(clientId.value(), provider.name());
    }
}

