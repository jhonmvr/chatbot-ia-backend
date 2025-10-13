package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.ports.messaging.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: Obtener historial de conversación
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetConversationHistory {
    
    private final MessageRepository messageRepository;
    
    /**
     * Obtiene el historial de mensajes de una conversación
     * 
     * @param conversationId ID de la conversación
     * @param limit Número máximo de mensajes a retornar
     * @return Lista de mensajes ordenados por fecha
     */
    public List<Message> handle(UuidId<Conversation> conversationId, int limit) {
        try {
            List<Message> messages = messageRepository.findByConversation(conversationId, limit);
            
            log.info("Recuperados {} mensajes para conversación {}", messages.size(), conversationId.value());
            
            return messages;
            
        } catch (Exception e) {
            log.error("Error al obtener historial de conversación {}: {}", 
                    conversationId.value(), e.getMessage(), e);
            return List.of();
        }
    }
}

