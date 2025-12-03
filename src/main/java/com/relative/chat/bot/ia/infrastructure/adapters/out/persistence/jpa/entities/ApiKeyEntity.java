package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "api_key", schema = "chatbotia")
public class ApiKeyEntity {
    
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "api_key", nullable = false, length = 255, unique = true)
    private String apiKey;
    
    @Column(name = "api_secret_hash", nullable = false, length = 255)
    private String apiSecretHash;
    
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", length = 30, nullable = false)
    private String status;
    
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;
}

