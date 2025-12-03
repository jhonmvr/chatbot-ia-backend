package com.relative.chat.bot.ia.domain.identity;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.types.EntityStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidad de dominio para API Keys de clientes
 * Representa las credenciales de autenticación para acceso a la API
 */
public final class ApiKey {
    
    private final UuidId<ApiKey> id;
    private final UuidId<Client> clientId;
    private final String apiKey; // Token público (identificador)
    private final String apiSecretHash; // Hash del secreto (no se almacena en texto plano)
    private final EntityStatus status;
    private final Instant createdAt;
    private final Instant lastUsedAt;
    
    private ApiKey(
            UuidId<ApiKey> id,
            UuidId<Client> clientId,
            String apiKey,
            String apiSecretHash,
            EntityStatus status,
            Instant createdAt,
            Instant lastUsedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.clientId = Objects.requireNonNull(clientId);
        this.apiKey = require(apiKey, "apiKey");
        this.apiSecretHash = require(apiSecretHash, "apiSecretHash");
        this.status = status == null ? EntityStatus.ACTIVE : status;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.lastUsedAt = lastUsedAt;
    }
    
    private static String require(String s, String fieldName) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(fieldName + " es requerido");
        }
        return s;
    }
    
    /**
     * Crea una nueva API Key
     */
    public static ApiKey create(
            UuidId<Client> clientId,
            String apiKey,
            String apiSecretHash
    ) {
        return new ApiKey(
                UuidId.newId(),
                clientId,
                apiKey,
                apiSecretHash,
                EntityStatus.ACTIVE,
                Instant.now(),
                null
        );
    }
    
    /**
     * Crea una API Key existente (para reconstruir desde persistencia)
     */
    public static ApiKey existing(
            UuidId<ApiKey> id,
            UuidId<Client> clientId,
            String apiKey,
            String apiSecretHash,
            EntityStatus status,
            Instant createdAt,
            Instant lastUsedAt
    ) {
        return new ApiKey(id, clientId, apiKey, apiSecretHash, status, createdAt, lastUsedAt);
    }
    
    /**
     * Marca la API Key como usada (actualiza lastUsedAt)
     */
    public ApiKey markAsUsed() {
        return new ApiKey(
                this.id,
                this.clientId,
                this.apiKey,
                this.apiSecretHash,
                this.status,
                this.createdAt,
                Instant.now()
        );
    }
    
    /**
     * Desactiva la API Key
     */
    public ApiKey deactivate() {
        return new ApiKey(
                this.id,
                this.clientId,
                this.apiKey,
                this.apiSecretHash,
                EntityStatus.INACTIVE,
                this.createdAt,
                this.lastUsedAt
        );
    }
    
    /**
     * Activa la API Key
     */
    public ApiKey activate() {
        return new ApiKey(
                this.id,
                this.clientId,
                this.apiKey,
                this.apiSecretHash,
                EntityStatus.ACTIVE,
                this.createdAt,
                this.lastUsedAt
        );
    }
    
    public boolean isActive() {
        return status == EntityStatus.ACTIVE;
    }
    
    // Getters
    public UuidId<ApiKey> id() {
        return id;
    }
    
    public UuidId<Client> clientId() {
        return clientId;
    }
    
    public String apiKey() {
        return apiKey;
    }
    
    public String apiSecretHash() {
        return apiSecretHash;
    }
    
    public EntityStatus status() {
        return status;
    }
    
    public Instant createdAt() {
        return createdAt;
    }
    
    public Instant lastUsedAt() {
        return lastUsedAt;
    }
}

