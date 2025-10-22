package com.relative.chat.bot.ia.domain.messaging;

/**
 * Botón de plantilla de WhatsApp
 */
public record TemplateButton(
    /**
     * Tipo de botón (URL, PHONE_NUMBER, QUICK_REPLY)
     */
    String type,
    
    /**
     * Texto del botón
     */
    String text,
    
    /**
     * URL del botón (para tipo URL)
     */
    String url,
    
    /**
     * Número de teléfono (para tipo PHONE_NUMBER)
     */
    String phoneNumber
) {
    /**
     * Constructor para botón de URL
     */
    public static TemplateButton urlButton(String text, String url) {
        return new TemplateButton("URL", text, url, null);
    }
    
    /**
     * Constructor para botón de teléfono
     */
    public static TemplateButton phoneButton(String text, String phoneNumber) {
        return new TemplateButton("PHONE_NUMBER", text, null, phoneNumber);
    }
    
    /**
     * Constructor para botón de respuesta rápida
     */
    public static TemplateButton quickReplyButton(String text) {
        return new TemplateButton("QUICK_REPLY", text, null, null);
    }
}
