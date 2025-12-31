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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad JPA para plantillas de WhatsApp
 */
@Getter
@Setter
@Entity
@Table(name = "whatsapp_templates", schema = "chatbotia")
public class WhatsAppTemplateEntity {
    
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_phone_id", nullable = false)
    private ClientPhoneEntity clientPhoneEntity;
    
    @Column(name = "name", nullable = false, length = 512)
    private String name;
    
    @Column(name = "category", nullable = false, length = 20)
    private String category;
    
    @Column(name = "language", nullable = false, length = 10)
    @ColumnDefault("'es_ES'")
    private String language;
    
    @Column(name = "status", nullable = false, length = 20)
    @ColumnDefault("'DRAFT'")
    private String status;
    
    @Column(name = "parameter_format", length = 20)
    private String parameterFormat;
    
    @Column(name = "meta_template_id", length = 100)
    private String metaTemplateId;
    
    @Column(name = "quality_rating", length = 20)
    @ColumnDefault("'PENDING'")
    private String qualityRating;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    /**
     * Código de error de rechazo de Meta (si está disponible)
     */
    @Column(name = "rejection_code", length = 50)
    private String rejectionCode;
    
    /**
     * Detalles adicionales del rechazo en formato JSON
     * Puede contener información estructurada sobre el motivo del rechazo
     */
    @Column(name = "rejection_details", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rejectionDetails;
    
    /**
     * Fecha y hora en que se produjo el rechazo
     */
    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;
    
    @ColumnDefault("'[]'::jsonb")
    @Column(name = "components", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> components;
    
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
