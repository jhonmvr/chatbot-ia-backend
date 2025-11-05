package com.relative.chat.bot.ia.domain.messaging;

/**
 * Calificaciones de calidad de plantillas según Meta API
 */
public enum QualityRating {
    /**
     * Calidad alta - sin comentarios negativos
     */
    HIGH,
    
    /**
     * Calidad media - algunos comentarios negativos
     */
    MEDIUM,
    
    /**
     * Calidad baja - muchos comentarios negativos
     */
    LOW,
    
    /**
     * Calidad pendiente - aún no evaluada
     */
    PENDING
}
