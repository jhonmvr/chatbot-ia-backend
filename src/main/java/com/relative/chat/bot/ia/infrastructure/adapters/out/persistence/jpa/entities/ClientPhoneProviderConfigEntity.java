package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad para configuraciones específicas de proveedores por cliente
 * Almacena los valores reales de configuración para cada número de WhatsApp
 */
@Getter
@Setter
@Entity
@Table(name = "client_phone_provider_config", schema = "chatbotia")
public class ClientPhoneProviderConfigEntity {
    
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_phone_id", nullable = false)
    private ClientPhoneEntity clientPhoneEntity;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "provider_config_id", nullable = false)
    private ProviderConfigEntity providerConfigEntity;
    
    /**
     * Valores de configuración específicos en formato JSON
     * Ejemplo para Meta:
     * {
     *   "access_token": "EAAxxxxxxxxxxxx",
     *   "phone_number_id": "123456789012345",
     *   "api_version": "v21.0",
     *   "webhook_secret": "mi_secreto_webhook",
     *   "verify_token": "mi_token_secreto"
     * }
     */
    @ColumnDefault("'{}'::jsonb")
    @Column(name = "config_values", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> configValues;
    
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
