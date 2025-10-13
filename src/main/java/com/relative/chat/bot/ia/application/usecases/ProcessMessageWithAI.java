package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.ports.out.AIService;
import com.relative.chat.bot.ia.application.ports.out.EmbeddingsPort;
import com.relative.chat.bot.ia.application.ports.out.VectorStore;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.ports.messaging.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Caso de uso: Procesar mensaje con IA
 * Busca contexto relevante en el knowledge base y genera una respuesta
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessMessageWithAI {
    
    private final EmbeddingsPort embeddingsPort;
    private final VectorStore vectorStore;
    private final AIService aiService;
    private final MessageRepository messageRepository;
    
    private static final int TOP_K_RESULTS = 5;
    private static final int MAX_CONVERSATION_HISTORY = 10;
    
    /**
     * Procesa un mensaje del usuario y genera una respuesta usando IA
     * 
     * @param userMessage Mensaje del usuario
     * @param conversationId ID de la conversación
     * @param namespace Namespace del knowledge base
     * @return Respuesta generada por la IA
     */
    public String handle(String userMessage, UuidId<Conversation> conversationId, String namespace) {
        try {
            // 1. Buscar contexto relevante en el knowledge base
            List<String> contextDocs = searchRelevantContext(userMessage, namespace);
            
            // 2. Obtener historial de conversación
            List<Map<String, String>> conversationHistory = getConversationHistory(conversationId);
            
            // 3. Generar respuesta con IA
            String response = aiService.generateResponse(userMessage, contextDocs, conversationHistory);
            
            log.info("Respuesta generada para conversación {}: {} caracteres", 
                    conversationId.value(), response.length());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error al procesar mensaje con IA: {}", e.getMessage(), e);
            return "Lo siento, ocurrió un error al procesar tu mensaje. ¿Puedes intentar de nuevo?";
        }
    }
    
    /**
     * Busca documentos relevantes en el knowledge base
     */
    private List<String> searchRelevantContext(String query, String namespace) {
        try {
            // Generar embedding de la consulta
            float[] queryEmbedding = embeddingsPort.embedOne(query);
            
            // Buscar documentos similares
            List<VectorStore.QueryResult> results = vectorStore.query(
                    namespace,
                    queryEmbedding,
                    TOP_K_RESULTS,
                    Map.of()
            );
            
            // Extraer el texto de los resultados
            return results.stream()
                    .filter(r -> r.payload() != null && r.payload().containsKey("text"))
                    .map(r -> (String) r.payload().get("text"))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.warn("Error al buscar contexto: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Obtiene el historial reciente de la conversación
     */
    private List<Map<String, String>> getConversationHistory(UuidId<Conversation> conversationId) {
        try {
            List<Message> messages = messageRepository.findByConversation(
                    conversationId,
                    MAX_CONVERSATION_HISTORY
            );
            
            List<Map<String, String>> history = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, String> entry = new HashMap<>();
                entry.put("role", msg.direction().name());
                entry.put("content", msg.content());
                history.add(entry);
            }
            
            return history;
            
        } catch (Exception e) {
            log.warn("Error al obtener historial: {}", e.getMessage());
            return List.of();
        }
    }
}

