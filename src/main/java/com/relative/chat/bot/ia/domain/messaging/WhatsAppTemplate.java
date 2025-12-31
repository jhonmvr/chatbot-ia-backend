package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Plantilla de WhatsApp Business API
 */
public record WhatsAppTemplate(
    /**
     * ID único de la plantilla
     */
    UuidId<WhatsAppTemplate> id,
    
    /**
     * ID del número de teléfono cliente asociado
     */
    UuidId<ClientPhone> clientPhoneId,
    
    /**
     * Nombre de la plantilla (máximo 512 caracteres alfanuméricos en minúscula y guiones bajos)
     */
    String name,
    
    /**
     * Categoría de la plantilla
     */
    TemplateCategory category,
    
    /**
     * Código de idioma (ej: es_ES, en_US)
     */
    String language,
    
    /**
     * Estado actual de la plantilla
     */
    TemplateStatus status,
    
    /**
     * Formato de parámetros (NAMED o POSITIONAL)
     */
    ParameterFormat parameterFormat,
    
    /**
     * Componentes de la plantilla
     */
    List<TemplateComponent> components,
    
    /**
     * ID de la plantilla en Meta API (cuando se sincroniza)
     */
    String metaTemplateId,
    
    /**
     * Calificación de calidad según Meta
     */
    QualityRating qualityRating,
    
    /**
     * Razón de rechazo (si aplica)
     */
    String rejectionReason,
    
    /**
     * Código de error de rechazo de Meta (si está disponible)
     */
    String rejectionCode,
    
    /**
     * Detalles adicionales del rechazo en formato JSON
     * Puede contener información estructurada sobre el motivo del rechazo
     */
    java.util.Map<String, Object> rejectionDetails,
    
    /**
     * Fecha y hora en que se produjo el rechazo
     */
    Instant rejectedAt,
    
    /**
     * Fecha de creación
     */
    Instant createdAt,
    
    /**
     * Fecha de última actualización
     */
    Instant updatedAt
) {
    /**
     * Constructor para nueva plantilla
     */
    public static WhatsAppTemplate create(
        UuidId<ClientPhone> clientPhoneId,
        String name,
        TemplateCategory category,
        String language,
        ParameterFormat parameterFormat,
        List<TemplateComponent> components
    ) {
        Instant now = Instant.now();
        return new WhatsAppTemplate(
            UuidId.newId(),
            clientPhoneId,
            name,
            category,
            language,
            TemplateStatus.DRAFT,
            parameterFormat,
            components,
            null,
            QualityRating.PENDING,
            null,
            null,
            null,
            null,
            now,
            now
        );
    }
    
    /**
     * Constructor para plantilla existente
     */
    public static WhatsAppTemplate existing(
        UuidId<WhatsAppTemplate> id,
        UuidId<ClientPhone> clientPhoneId,
        String name,
        TemplateCategory category,
        String language,
        TemplateStatus status,
        ParameterFormat parameterFormat,
        List<TemplateComponent> components,
        String metaTemplateId,
        QualityRating qualityRating,
        String rejectionReason,
        String rejectionCode,
        java.util.Map<String, Object> rejectionDetails,
        Instant rejectedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new WhatsAppTemplate(
            id, clientPhoneId, name, category, language, status, parameterFormat,
            components, metaTemplateId, qualityRating, rejectionReason, rejectionCode,
            rejectionDetails, rejectedAt, createdAt, updatedAt
        );
    }
    
    /**
     * Actualiza el estado de la plantilla
     */
    public WhatsAppTemplate withStatus(TemplateStatus newStatus) {
        return new WhatsAppTemplate(
            id, clientPhoneId, name, category, language, newStatus, parameterFormat,
            components, metaTemplateId, qualityRating, rejectionReason, rejectionCode,
            rejectionDetails, rejectedAt, createdAt, Instant.now()
        );
    }
    
    /**
     * Actualiza el ID de Meta
     */
    public WhatsAppTemplate withMetaTemplateId(String metaId) {
        return new WhatsAppTemplate(
            id, clientPhoneId, name, category, language, status, parameterFormat,
            components, metaId, qualityRating, rejectionReason, rejectionCode,
            rejectionDetails, rejectedAt, createdAt, Instant.now()
        );
    }
    
    /**
     * Actualiza la calificación de calidad
     */
    public WhatsAppTemplate withQualityRating(QualityRating rating) {
        return new WhatsAppTemplate(
            id, clientPhoneId, name, category, language, status, parameterFormat,
            components, metaTemplateId, rating, rejectionReason, rejectionCode,
            rejectionDetails, rejectedAt, createdAt, Instant.now()
        );
    }
    
    /**
     * Actualiza la razón de rechazo
     */
    public WhatsAppTemplate withRejectionReason(String reason) {
        return new WhatsAppTemplate(
            id, clientPhoneId, name, category, language, status, parameterFormat,
            components, metaTemplateId, qualityRating, reason, rejectionCode,
            rejectionDetails, rejectedAt, createdAt, Instant.now()
        );
    }
    
    /**
     * Actualiza toda la información de rechazo
     */
    public WhatsAppTemplate withRejectionInfo(String reason, String code, java.util.Map<String, Object> details) {
        Instant now = Instant.now();
        return new WhatsAppTemplate(
            id, clientPhoneId, name, category, language, status, parameterFormat,
            components, metaTemplateId, qualityRating, reason, code, details, now,
            createdAt, Instant.now()
        );
    }
    
    /**
     * Limpia toda la información de rechazo
     */
    public WhatsAppTemplate clearRejectionInfo() {
        return new WhatsAppTemplate(
            id, clientPhoneId, name, category, language, status, parameterFormat,
            components, metaTemplateId, qualityRating, null, null, null, null,
            createdAt, Instant.now()
        );
    }
    
    /**
     * Obtiene metaTemplateId como Optional
     */
    public Optional<String> metaTemplateIdOpt() {
        return Optional.ofNullable(metaTemplateId);
    }
    
    /**
     * Obtiene rejectionReason como Optional
     */
    public Optional<String> rejectionReasonOpt() {
        return Optional.ofNullable(rejectionReason);
    }
    
    /**
     * Obtiene rejectionCode como Optional
     */
    public Optional<String> rejectionCodeOpt() {
        return Optional.ofNullable(rejectionCode);
    }
    
    /**
     * Obtiene rejectionDetails como Optional
     */
    public Optional<java.util.Map<String, Object>> rejectionDetailsOpt() {
        return Optional.ofNullable(rejectionDetails);
    }
    
    /**
     * Obtiene rejectedAt como Optional
     */
    public Optional<Instant> rejectedAtOpt() {
        return Optional.ofNullable(rejectedAt);
    }
    
    /**
     * Verifica si la plantilla está aprobada
     */
    public boolean isApproved() {
        return status == TemplateStatus.APPROVED;
    }
    
    /**
     * Verifica si la plantilla está sincronizada con Meta
     */
    public boolean isSyncedWithMeta() {
        return metaTemplateId != null && !metaTemplateId.isEmpty();
    }
    
    /**
     * Verifica si la plantilla puede ser enviada
     */
    public boolean canBeSent() {
        return status == TemplateStatus.APPROVED && isSyncedWithMeta();
    }
}
