package com.relative.chat.bot.ia.domain.messaging;

/**
 * Estados de plantillas de WhatsApp según Meta API
 */
public enum TemplateStatus {
    /**
     * Borrador - plantilla creada localmente pero no enviada a Meta
     */
    DRAFT,
    
    /**
     * En revisión - plantilla enviada a Meta y bajo revisión (hasta 24 horas)
     */
    IN_REVIEW,
    
    /**
     * Pendiente - enviada a Meta y en proceso de revisión
     */
    PENDING,
    
    /**
     * Aprobada - plantilla aprobada por Meta y lista para usar
     */
    APPROVED,
    
    /**
     * Rechazada - plantilla rechazada por Meta
     */
    REJECTED,
    
    /**
     * Pausada - plantilla pausada por Meta debido a baja calidad
     */
    PAUSED,
    
    /**
     * Desactivada - plantilla desactivada por Meta
     */
    DISABLED,
    
    /**
     * Apelación solicitada - se ha solicitado una apelación
     */
    APPEAL_REQUESTED
}
