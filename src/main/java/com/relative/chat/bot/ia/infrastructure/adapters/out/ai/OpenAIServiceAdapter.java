package com.relative.chat.bot.ia.infrastructure.adapters.out.ai;

import com.relative.chat.bot.ia.application.ports.out.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación de AIService usando Spring AI con OpenAI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIServiceAdapter implements AIService {
    
    private final ChatClient.Builder chatClientBuilder;
    
    private static final String SYSTEM_PROMPT = """
            Eres un asistente virtual inteligente y útil que responde preguntas basándote en la información proporcionada.
            
            Instrucciones:
            - Responde de manera clara, concisa y profesional
            - Usa el contexto proporcionado para dar respuestas precisas
            - Si no tienes suficiente información, admítelo y ofrece ayuda alternativa
            - Mantén un tono amigable pero profesional
            - Si hay historial de conversación, úsalo para dar respuestas contextualizadas
            """;
    
    @Override
    public String generateResponse(
            String userMessage,
            List<String> context,
            List<Map<String, String>> conversationHistory
    ) {
        try {
            // Construir el prompt con contexto
            String contextStr = buildContextString(context);
            String historyStr = buildHistoryString(conversationHistory);
            
            String enhancedPrompt = String.format("""
                    Contexto relevante:
                    %s
                    
                    Historial de conversación:
                    %s
                    
                    Pregunta del usuario: %s
                    
                    Responde basándote en el contexto y el historial proporcionado.
                    """, contextStr, historyStr, userMessage);
            
            ChatClient chatClient = chatClientBuilder.build();
            
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(enhancedPrompt)
                    .call()
                    .content();
            
            log.info("Respuesta generada exitosamente para mensaje: {}", 
                    userMessage.substring(0, Math.min(50, userMessage.length())));
            
            return response != null ? response : "Lo siento, no pude generar una respuesta.";
            
        } catch (Exception e) {
            log.error("Error al generar respuesta con IA: {}", e.getMessage(), e);
            return "Lo siento, ocurrió un error al procesar tu mensaje. ¿Puedes intentar de nuevo?";
        }
    }
    
    @Override
    public String generateSimpleResponse(String userMessage) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            
            String response = chatClient.prompt()
                    .system("Eres un asistente virtual amigable y útil.")
                    .user(userMessage)
                    .call()
                    .content();
            
            return response != null ? response : "Lo siento, no pude generar una respuesta.";
            
        } catch (Exception e) {
            log.error("Error al generar respuesta simple: {}", e.getMessage(), e);
            return "Lo siento, ocurrió un error. ¿Puedes intentar de nuevo?";
        }
    }
    
    /**
     * Construye una cadena de texto con el contexto relevante
     */
    private String buildContextString(List<String> context) {
        if (context == null || context.isEmpty()) {
            return "No hay contexto disponible.";
        }
        
        return context.stream()
                .limit(5) // Limitar a los 5 documentos más relevantes
                .map(doc -> "- " + doc)
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Construye una cadena de texto con el historial de conversación
     */
    private String buildHistoryString(List<Map<String, String>> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return "Esta es una nueva conversación.";
        }
        
        return conversationHistory.stream()
                .limit(10) // Limitar a los últimos 10 mensajes
                .map(entry -> {
                    String role = entry.getOrDefault("role", "UNKNOWN");
                    String content = entry.getOrDefault("content", "");
                    String roleLabel = role.equals("IN") ? "Usuario" : "Asistente";
                    return String.format("%s: %s", roleLabel, content);
                })
                .collect(Collectors.joining("\n"));
    }
}

