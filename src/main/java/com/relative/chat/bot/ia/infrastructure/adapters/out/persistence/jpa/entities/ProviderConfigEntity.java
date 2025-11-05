package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad para configuraciones parametrizables de proveedores de WhatsApp
 * Permite agregar nuevos proveedores sin modificar el código
 */
@Getter
@Setter
@Entity
@Table(name = "provider_config", schema = "chatbotia")
public class ProviderConfigEntity {
    
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;
    
    @Column(name = "provider_type", nullable = false, length = 20)
    private String providerType; // META, TWILIO, WWEBJS, etc.
    
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "api_base_url", length = 500)
    private String apiBaseUrl;
    
    @Column(name = "api_version", length = 20)
    private String apiVersion;
    
    @Column(name = "webhook_url_template", length = 500)
    private String webhookUrlTemplate;
    
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private Boolean isActive = true;
    
    @Column(name = "is_default", nullable = false)
    @ColumnDefault("false")
    private Boolean isDefault = false;
    
    /**
     * Configuración parametrizable en formato JSON
     * Ejemplo para Meta:
     * {
     *   "required_fields": ["access_token", "phone_number_id"],
     *   "optional_fields": ["api_version", "webhook_secret", "verify_token"],
     *   "field_configs": {
     *     "access_token": {
     *       "type": "string",
     *       "max_length": 500,
     *       "description": "Token de acceso de Meta WhatsApp Business API"
     *     },
     *     "phone_number_id": {
     *       "type": "string",
     *       "max_length": 50,
     *       "description": "ID del número de teléfono en Meta"
     *     }
     *   }
     * }
     */
    @ColumnDefault("'{}'::jsonb")
    @Column(name = "config_schema", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> configSchema;
    
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
