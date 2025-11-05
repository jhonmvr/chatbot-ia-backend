package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.ProviderConfig;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para ProviderConfig
 */
public interface ProviderConfigRepository {
    
    /**
     * Busca un proveedor por ID
     */
    Optional<ProviderConfig> findById(UuidId<ProviderConfig> id);
    
    /**
     * Busca un proveedor por tipo
     */
    Optional<ProviderConfig> findByProviderType(String providerType);
    
    /**
     * Busca todos los proveedores activos
     */
    List<ProviderConfig> findActiveProviders();
    
    /**
     * Busca el proveedor por defecto
     */
    Optional<ProviderConfig> findDefaultProvider();
    
    /**
     * Guarda un proveedor
     */
    void save(ProviderConfig providerConfig);
    
    /**
     * Obtiene todos los proveedores
     */
    List<ProviderConfig> findAll();
    
    /**
     * Elimina un proveedor
     */
    void delete(UuidId<ProviderConfig> id);
}
