package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Obtener todas las conversaciones de un contacto
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetContactConversations {
    
    private final ConversationRepository conversationRepository;
    
    /**
     * Obtiene todas las conversaciones de un contacto específico
     * 
     * @param contactId ID del contacto
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @return Resultado con paginación
     */
    public ConversationRepository.SearchResult handle(
            UuidId<Contact> contactId,
            int page,
            int size
    ) {
        try {
            ConversationRepository.SearchResult result = conversationRepository.findByContact(
                contactId,
                page,
                size
            );
            
            log.info("Conversaciones del contacto {}: {} encontradas, página {}, tamaño {}", 
                contactId.value(), result.total(), page, size);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error al obtener conversaciones del contacto {}: {}", 
                contactId.value(), e.getMessage(), e);
            return new ConversationRepository.SearchResult(
                java.util.List.of(),
                0L,
                page,
                size,
                0
            );
        }
    }
}

