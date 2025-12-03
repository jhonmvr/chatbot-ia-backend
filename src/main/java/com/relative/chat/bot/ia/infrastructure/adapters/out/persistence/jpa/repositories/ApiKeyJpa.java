package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyJpa extends JpaRepository<ApiKeyEntity, UUID> {
    
    /**
     * Busca una API Key por su apiKey (token público)
     */
    Optional<ApiKeyEntity> findByApiKey(String apiKey);
    
    /**
     * Busca API Keys por clientId
     */
    java.util.List<ApiKeyEntity> findByClientId(UUID clientId);
    
    /**
     * Busca API Keys activas por clientId
     */
    java.util.List<ApiKeyEntity> findByClientIdAndStatus(UUID clientId, String status);
    
    /**
     * Verifica si existe una API Key con el apiKey dado
     */
    boolean existsByApiKey(String apiKey);
}

