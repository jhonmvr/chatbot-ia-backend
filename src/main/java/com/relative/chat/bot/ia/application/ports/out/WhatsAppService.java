package com.relative.chat.bot.ia.application.ports.out;

/**
 * Puerto para enviar mensajes a WhatsApp a través de proveedor externo (Twilio, etc.)
 */
public interface WhatsAppService {
    
    /**
     * Envía un mensaje de texto a través de WhatsApp
     * 
     * @param from Número de teléfono origen (formato E.164)
     * @param to Número de teléfono destino (formato E.164)
     * @param message Contenido del mensaje
     * @return ID externo del mensaje enviado
     */
    String sendMessage(String from, String to, String message);
    
    /**
     * Envía un mensaje con plantilla
     * 
     * @param from Número de teléfono origen
     * @param to Número de teléfono destino
     * @param templateId ID de la plantilla
     * @param parameters Parámetros de la plantilla
     * @return ID externo del mensaje enviado
     */
    String sendTemplate(String from, String to, String templateId, java.util.Map<String, String> parameters);
}

