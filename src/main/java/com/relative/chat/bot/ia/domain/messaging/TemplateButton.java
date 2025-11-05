package com.relative.chat.bot.ia.domain.messaging;

/**
 * Botón de plantilla de WhatsApp
 */
public record TemplateButton(
    /**
     * Tipo de botón (URL, PHONE_NUMBER, QUICK_REPLY, OTP, CATALOG, MPM)
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
    String phoneNumber,
    
    /**
     * Para botones OTP: tipo de OTP (COPY_CODE, ONE_TAP)
     */
    String otpType,
    
    /**
     * Para botones OTP ONE_TAP: texto de autofill
     */
    String autofillText,
    
    /**
     * Para botones OTP ONE_TAP: nombre del paquete Android
     */
    String packageName,
    
    /**
     * Para botones OTP ONE_TAP: hash de firma de la app
     */
    String signatureHash,
    
    /**
     * Para botones URL dinámicos: ejemplo de código/promoción
     */
    String example
) {
    /**
     * Constructor por defecto
     */
    public TemplateButton {
        // Constructor compacto
    }
    
    /**
     * Constructor para botón de URL
     */
    public static TemplateButton urlButton(String text, String url) {
        return new TemplateButton("URL", text, url, null, null, null, null, null, null);
    }
    
    /**
     * Constructor para botón de URL dinámico
     */
    public static TemplateButton urlButtonDynamic(String text, String url, String example) {
        return new TemplateButton("URL", text, url, null, null, null, null, null, example);
    }
    
    /**
     * Constructor para botón de teléfono
     */
    public static TemplateButton phoneButton(String text, String phoneNumber) {
        return new TemplateButton("PHONE_NUMBER", text, null, phoneNumber, null, null, null, null, null);
    }
    
    /**
     * Constructor para botón de respuesta rápida
     */
    public static TemplateButton quickReplyButton(String text) {
        return new TemplateButton("QUICK_REPLY", text, null, null, null, null, null, null, null);
    }
    
    /**
     * Constructor para botón OTP COPY_CODE
     */
    public static TemplateButton otpCopyCodeButton(String text) {
        return new TemplateButton("OTP", text, null, null, "COPY_CODE", null, null, null, null);
    }
    
    /**
     * Constructor para botón OTP ONE_TAP
     */
    public static TemplateButton otpOneTapButton(String text, String autofillText, String packageName, String signatureHash) {
        return new TemplateButton("OTP", text, null, null, "ONE_TAP", autofillText, packageName, signatureHash, null);
    }
    
    /**
     * Constructor para botón CATALOG
     */
    public static TemplateButton catalogButton(String text) {
        return new TemplateButton("CATALOG", text, null, null, null, null, null, null, null);
    }
    
    /**
     * Constructor para botón MPM (Multi-Product Message)
     */
    public static TemplateButton mpmButton(String text) {
        return new TemplateButton("MPM", text, null, null, null, null, null, null, null);
    }
}
