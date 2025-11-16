package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.dto.*;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.ports.identity.ClientRepository;
import com.relative.chat.bot.ia.domain.ports.scheduling.CalendarProviderAccountRepository;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProvider;
import com.relative.chat.bot.ia.domain.scheduling.CalendarProviderAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Caso de uso para gestionar cuentas de proveedores de calendario
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManageCalendarProviderAccount {
    
    private final CalendarProviderAccountRepository repository;
    private final ClientRepository clientRepository;
    
    /**
     * Crea una nueva cuenta de proveedor de calendario
     */
    public CalendarProviderAccountResponse create(CreateCalendarProviderAccountRequest request) {
        log.info("Creando cuenta de calendario: clientId={}, provider={}, email={}", 
                request.clientId(), request.provider(), request.accountEmail());
        
        // Validar que el cliente existe
        UuidId<Client> clientId = UuidId.of(java.util.UUID.fromString(request.clientId()));
        clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + request.clientId()));
        
        // Verificar si ya existe una cuenta activa para este cliente y proveedor
        Optional<CalendarProviderAccount> existing = repository.findActiveByClientIdAndProvider(clientId, request.provider());
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("Ya existe una cuenta activa de %s para este cliente", request.provider().getDisplayName())
            );
        }
        
        // Crear nueva cuenta
        CalendarProviderAccount account = new CalendarProviderAccount(
                UuidId.newId(),
                clientId,
                request.provider(),
                request.accountEmail(),
                request.accessToken(),
                request.refreshToken(),
                request.tokenExpiresAt(),
                request.config(),
                request.isActive(),
                Instant.now(),
                Instant.now()
        );
        
        repository.save(account);
        
        log.info("Cuenta de calendario creada: id={}", account.id().value());
        
        return toResponse(account);
    }
    
    /**
     * Actualiza una cuenta existente
     */
    public CalendarProviderAccountResponse update(String accountId, UpdateCalendarProviderAccountRequest request) {
        log.info("Actualizando cuenta de calendario: id={}", accountId);
        
        UuidId<CalendarProviderAccount> id = UuidId.of(java.util.UUID.fromString(accountId));
        CalendarProviderAccount existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + accountId));
        
        // Actualizar campos si están presentes
        CalendarProviderAccount updated = existing;
        
        if (request.accessToken() != null) {
            updated = updated.withTokens(
                    request.accessToken(),
                    request.refreshToken() != null ? request.refreshToken() : existing.refreshToken(),
                    request.tokenExpiresAt() != null ? request.tokenExpiresAt() : existing.tokenExpiresAt()
            );
        } else if (request.refreshToken() != null || request.tokenExpiresAt() != null) {
            updated = updated.withTokens(
                    existing.accessToken(),
                    request.refreshToken() != null ? request.refreshToken() : existing.refreshToken(),
                    request.tokenExpiresAt() != null ? request.tokenExpiresAt() : existing.tokenExpiresAt()
            );
        }
        
        if (request.config() != null) {
            updated = updated.withConfig(request.config());
        }
        
        if (request.isActive() != null) {
            updated = updated.withActive(request.isActive());
        }
        
        repository.save(updated);
        
        log.info("Cuenta de calendario actualizada: id={}", accountId);
        
        return toResponse(updated);
    }
    
    /**
     * Obtiene una cuenta por ID
     */
    public Optional<CalendarProviderAccountResponse> findById(String accountId) {
        UuidId<CalendarProviderAccount> id = UuidId.of(java.util.UUID.fromString(accountId));
        return repository.findById(id)
                .map(this::toResponse);
    }
    
    /**
     * Obtiene todas las cuentas de un cliente
     */
    public List<CalendarProviderAccountResponse> findByClientId(String clientId) {
        UuidId<Client> id = UuidId.of(java.util.UUID.fromString(clientId));
        return repository.findByClientId(id)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene cuentas activas de un cliente
     */
    public List<CalendarProviderAccountResponse> findActiveByClientId(String clientId) {
        UuidId<Client> id = UuidId.of(java.util.UUID.fromString(clientId));
        return repository.findActiveByClientId(id)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene cuenta activa por cliente y proveedor
     */
    public Optional<CalendarProviderAccountResponse> findActiveByClientIdAndProvider(String clientId, CalendarProvider provider) {
        UuidId<Client> id = UuidId.of(java.util.UUID.fromString(clientId));
        return repository.findActiveByClientIdAndProvider(id, provider)
                .map(this::toResponse);
    }
    
    /**
     * Elimina una cuenta
     */
    public void delete(String accountId) {
        log.info("Eliminando cuenta de calendario: id={}", accountId);
        
        UuidId<CalendarProviderAccount> id = UuidId.of(java.util.UUID.fromString(accountId));
        repository.delete(id);
        
        log.info("Cuenta de calendario eliminada: id={}", accountId);
    }
    
    /**
     * Convierte CalendarProviderAccount a CalendarProviderAccountResponse
     */
    private CalendarProviderAccountResponse toResponse(CalendarProviderAccount account) {
        return new CalendarProviderAccountResponse(
                account.id().value().toString(),
                account.clientId().value().toString(),
                account.provider(),
                account.accountEmail(),
                account.tokenExpiresAt(),
                account.config(),
                account.isActive(),
                account.createdAt(),
                account.updatedAt(),
                account.isTokenExpired()
        );
    }
}

