package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Listar y buscar conversaciones
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListConversations {
    
    private final ConversationRepository conversationRepository;
    
    /**
     * Lista y busca conversaciones con filtros opcionales
     * 
     * @param clientId ID del cliente (opcional)
     * @param query Texto de búsqueda (busca en títulos, mensajes y contactos)
     * @param contactId ID del contacto para filtrar (opcional)
     * @param status Estado de la conversación (opcional)
     * @param channel Canal de comunicación (opcional)
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @return Resultado de búsqueda con paginación
     */
    public ConversationRepository.SearchResult handle(
            UuidId<Client> clientId,
            String query,
            UuidId<Contact> contactId,
            String status,
            Channel channel,
            int page,
            int size
    ) {
        try {
            ConversationRepository.SearchResult result = conversationRepository.searchConversations(
                clientId,
                query,
                contactId,
                status,
                channel,
                page,
                size
            );
            
            log.info("Búsqueda de conversaciones: {} encontradas, página {}, tamaño {}", 
                result.total(), page, size);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error al buscar conversaciones: {}", e.getMessage(), e);
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

