package com.relative.chat.bot.ia.domain.messaging;

/**
 * Formatos de parámetros para plantillas de WhatsApp
 */
public enum ParameterFormat {
    /**
     * Parámetros con nombre - {{first_name}}, {{order_number}}
     */
    NAMED,
    
    /**
     * Parámetros posicionales - {{1}}, {{2}}
     */
    POSITIONAL
}
