package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Caso de uso: Cerrar conversación
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloseConversation {
    
    private final ConversationRepository conversationRepository;
    
    /**
     * Cierra una conversación existente
     * 
     * @param conversationId ID de la conversación
     * @return true si se cerró exitosamente, false en caso contrario
     */
    @Transactional
    public boolean handle(UuidId<Conversation> conversationId) {
        try {
            Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
            
            if (conversationOpt.isEmpty()) {
                log.warn("Conversación no encontrada: {}", conversationId.value());
                return false;
            }
            
            Conversation conversation = conversationOpt.get();
            conversation.close(Instant.now());
            
            conversationRepository.save(conversation);
            
            log.info("Conversación cerrada exitosamente: {}", conversationId.value());
            return true;
            
        } catch (Exception e) {
            log.error("Error al cerrar conversación {}: {}", conversationId.value(), e.getMessage(), e);
            return false;
        }
    }
}

