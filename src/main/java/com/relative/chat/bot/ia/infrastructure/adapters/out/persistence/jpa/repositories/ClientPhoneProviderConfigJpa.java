package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneProviderConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para ClientPhoneProviderConfig
 */
public interface ClientPhoneProviderConfigJpa extends JpaRepository<ClientPhoneProviderConfigEntity, UUID> {
    
    /**
     * Buscar configuraciones por client_phone_id
     */
    List<ClientPhoneProviderConfigEntity> findByClientPhoneEntityId(UUID clientPhoneId);
    
    /**
     * Buscar configuración activa por client_phone_id
     */
    List<ClientPhoneProviderConfigEntity> findByClientPhoneEntityIdAndIsActiveTrue(UUID clientPhoneId);
    
    /**
     * Buscar configuración específica por client_phone_id y provider_config_id
     */
    Optional<ClientPhoneProviderConfigEntity> findByClientPhoneEntityIdAndProviderConfigEntityId(UUID clientPhoneId, UUID providerConfigId);
    
    /**
     * Buscar configuraciones por provider_config_id
     */
    List<ClientPhoneProviderConfigEntity> findByProviderConfigEntityId(UUID providerConfigId);
    
    /**
     * Buscar configuración activa por client_phone_id y tipo de proveedor
     */
    @Query("SELECT cppc FROM ClientPhoneProviderConfigEntity cppc " +
           "JOIN cppc.providerConfigEntity pc " +
           "WHERE cppc.clientPhoneEntity.id = :clientPhoneId " +
           "AND pc.providerType = :providerType " +
           "AND cppc.isActive = true")
    Optional<ClientPhoneProviderConfigEntity> findByClientPhoneIdAndProviderType(@Param("clientPhoneId") UUID clientPhoneId, @Param("providerType") String providerType);
    
    /**
     * Verificar si existe configuración para un client_phone y provider
     */
    boolean existsByClientPhoneEntityIdAndProviderConfigEntityId(UUID clientPhoneId, UUID providerConfigId);
}
