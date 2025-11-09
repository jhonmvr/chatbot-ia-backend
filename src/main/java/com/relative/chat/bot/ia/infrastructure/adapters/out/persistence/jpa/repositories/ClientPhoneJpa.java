package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para ClientPhone
 */
public interface ClientPhoneJpa extends JpaRepository<ClientPhoneEntity, UUID> {
    
    /**
     * Buscar todos los teléfonos de un cliente
     */
    List<ClientPhoneEntity> findByClientEntityId(UUID clientId);

    /**
     * Buscar por provider SID y provider
     * Este método es CLAVE para identificar al cliente desde webhooks de WhatsApp
     */
    Optional<ClientPhoneEntity> findByProviderSidAndProvider(String providerSid, String provider);

    /**
     * Buscar por provider SID y provider
     * Este método es CLAVE para identificar al cliente desde webhooks de WhatsApp
     */
    Optional<ClientPhoneEntity> findByProviderSid(String providerSid);

    /**
     * Buscar por número E164 y canal
     */
    Optional<ClientPhoneEntity> findByE164AndChannel(String e164, String channel);
    
    /**
     * Verificar si existe un número para un proveedor
     */
    boolean existsByProviderSidAndProvider(String providerSid, String provider);
}
