package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ProviderConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para ProviderConfig
 */
public interface ProviderConfigJpa extends JpaRepository<ProviderConfigEntity, UUID> {
    
    /**
     * Buscar por tipo de proveedor
     */
    Optional<ProviderConfigEntity> findByProviderType(String providerType);
    
    /**
     * Buscar proveedores activos
     */
    List<ProviderConfigEntity> findByIsActiveTrue();
    
    /**
     * Buscar proveedor por defecto
     */
    Optional<ProviderConfigEntity> findByIsDefaultTrue();
    
    /**
     * Verificar si existe un proveedor por tipo
     */
    boolean existsByProviderType(String providerType);
}
