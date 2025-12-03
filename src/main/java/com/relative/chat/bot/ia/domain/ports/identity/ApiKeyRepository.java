package com.relative.chat.bot.ia.domain.ports.identity;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.ApiKey;
import com.relative.chat.bot.ia.domain.identity.Client;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para API Keys
 */
public interface ApiKeyRepository {
    
    /**
     * Busca una API Key por su ID
     */
    Optional<ApiKey> findById(UuidId<ApiKey> id);
    
    /**
     * Busca una API Key por su apiKey (token público)
     */
    Optional<ApiKey> findByApiKey(String apiKey);
    
    /**
     * Busca API Keys por clientId
     */
    List<ApiKey> findByClientId(UuidId<Client> clientId);
    
    /**
     * Busca API Keys activas por clientId
     */
    List<ApiKey> findActiveByClientId(UuidId<Client> clientId);
    
    /**
     * Guarda una API Key (crea o actualiza)
     */
    void save(ApiKey apiKey);
    
    /**
     * Elimina una API Key
     */
    void delete(UuidId<ApiKey> id);
    
    /**
     * Verifica si existe una API Key con el apiKey dado
     */
    boolean existsByApiKey(String apiKey);
}

