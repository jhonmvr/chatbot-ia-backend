package com.relative.chat.bot.ia.application.ports.out;

import java.util.List;
import java.util.Map;

/**
 * Puerto para servicios de IA (OpenAI, etc.)
 */
public interface AIService {
    
    /**
     * Genera una respuesta usando el modelo de IA con contexto
     * 
     * @param userMessage Mensaje del usuario
     * @param context Contexto relevante recuperado del knowledge base
     * @param conversationHistory Historial de conversación
     * @return Respuesta generada por la IA
     */
    String generateResponse(
            String userMessage,
            List<String> context,
            List<Map<String, String>> conversationHistory
    );
    
    /**
     * Genera una respuesta simple sin contexto
     * 
     * @param userMessage Mensaje del usuario
     * @return Respuesta generada por la IA
     */
    String generateSimpleResponse(String userMessage);
}

