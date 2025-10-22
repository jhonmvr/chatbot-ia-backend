package com.relative.chat.bot.ia.domain.messaging;

/**
 * Tipos de componentes de plantillas de WhatsApp
 */
public enum ComponentType {
    /**
     * Encabezado - texto, imagen, video, documento
     */
    HEADER,
    
    /**
     * Cuerpo - texto principal con parámetros
     */
    BODY,
    
    /**
     * Pie de página - texto al final
     */
    FOOTER,
    
    /**
     * Botones - botones de acción
     */
    BUTTONS
}
