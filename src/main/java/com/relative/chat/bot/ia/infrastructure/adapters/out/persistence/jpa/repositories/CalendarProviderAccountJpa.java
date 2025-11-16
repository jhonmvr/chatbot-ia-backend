package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.CalendarProviderAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para CalendarProviderAccount
 */
public interface CalendarProviderAccountJpa extends JpaRepository<CalendarProviderAccountEntity, UUID> {
    
    /**
     * Buscar cuentas por client_id
     */
    List<CalendarProviderAccountEntity> findByClientEntityId(UUID clientId);
    
    /**
     * Buscar cuentas activas por client_id
     */
    List<CalendarProviderAccountEntity> findByClientEntityIdAndIsActiveTrue(UUID clientId);
    
    /**
     * Buscar cuenta activa por client_id y provider
     */
    Optional<CalendarProviderAccountEntity> findByClientEntityIdAndProviderAndIsActiveTrue(UUID clientId, String provider);
    
    /**
     * Buscar cuenta por client_id y provider (sin importar si está activa)
     */
    Optional<CalendarProviderAccountEntity> findByClientEntityIdAndProvider(UUID clientId, String provider);
    
    /**
     * Buscar cuenta por account_email y provider
     */
    Optional<CalendarProviderAccountEntity> findByAccountEmailAndProvider(String accountEmail, String provider);
    
    /**
     * Verificar si existe cuenta activa para un cliente y proveedor
     */
    boolean existsByClientEntityIdAndProviderAndIsActiveTrue(UUID clientId, String provider);
}

