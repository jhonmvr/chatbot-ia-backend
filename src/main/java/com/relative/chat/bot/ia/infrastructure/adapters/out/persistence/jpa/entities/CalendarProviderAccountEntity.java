package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad JPA para cuentas de proveedores de calendario
 * Almacena las credenciales OAuth2 y configuración para Google Calendar y Outlook
 */
@Getter
@Setter
@Entity
@Table(name = "calendar_provider_account", schema = "chatbotia")
public class CalendarProviderAccountEntity {
    
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity clientEntity;
    
    @Column(name = "provider", nullable = false, length = 20)
    private String provider; // "GOOGLE" o "OUTLOOK"
    
    @Column(name = "account_email", nullable = false, length = 255)
    private String accountEmail;
    
    @Column(name = "access_token", nullable = false, length = 2000)
    private String accessToken;
    
    @Column(name = "refresh_token", nullable = false, length = 2000)
    private String refreshToken;
    
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;
    
    /**
     * Configuración adicional en formato JSON
     * Ejemplo para Google:
     * {
     *   "calendar_id": "primary",
     *   "impersonated_email": "admin@example.com",
     *   "timezone": "America/Guayaquil"
     * }
     * Ejemplo para Outlook:
     * {
     *   "calendar_id": "AAMkAGI2...",
     *   "tenant_id": "12345678-1234-1234-1234-123456789012",
     *   "timezone": "America/Guayaquil"
     * }
     */
    @ColumnDefault("'{}'::jsonb")
    @Column(name = "config", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> config;
    
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private Boolean isActive = true;
    
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

