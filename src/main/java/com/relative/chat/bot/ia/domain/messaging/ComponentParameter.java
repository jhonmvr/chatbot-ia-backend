package com.relative.chat.bot.ia.domain.messaging;

/**
 * Parámetro de componente de plantilla
 */
public record ComponentParameter(
    /**
     * Tipo de parámetro (text, currency, date_time, etc.)
     */
    String type,
    
    /**
     * Texto del parámetro
     */
    String text,
    
    /**
     * Nombre del parámetro (para formato NAMED)
     */
    String parameterName,
    
    /**
     * Ejemplo del parámetro
     */
    String example
) {
    /**
     * Constructor para parámetros posicionales
     */
    public static ComponentParameter positional(String type, String text, String example) {
        return new ComponentParameter(type, text, null, example);
    }
    
    /**
     * Constructor para parámetros con nombre
     */
    public static ComponentParameter named(String type, String text, String parameterName, String example) {
        return new ComponentParameter(type, text, parameterName, example);
    }
}
