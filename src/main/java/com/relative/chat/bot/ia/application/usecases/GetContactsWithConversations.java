package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Obtener contactos únicos con conversaciones, ordenados por mensaje más reciente
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetContactsWithConversations {
    
    private final ConversationRepository conversationRepository;
    
    /**
     * Obtiene contactos únicos que tengan conversaciones, ordenados por el mensaje más reciente
     * 
     * @param clientId ID del cliente (requerido)
     * @param query Texto de búsqueda (busca en nombre del contacto, teléfono y contenido de mensajes)
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @return Resultado con información del contacto, última conversación y último mensaje
     */
    public ConversationRepository.ContactConversationResult handle(
            UuidId<Client> clientId,
            String query,
            int page,
            int size
    ) {
        try {
            ConversationRepository.ContactConversationResult result = conversationRepository.findContactsWithConversations(
                clientId,
                query,
                page,
                size
            );
            
            log.info("Contactos con conversaciones: {} encontrados, página {}, tamaño {}", 
                result.total(), page, size);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error al obtener contactos con conversaciones: {}", e.getMessage(), e);
            return new ConversationRepository.ContactConversationResult(
                java.util.List.of(),
                0L,
                page,
                size,
                0
            );
        }
    }
}


