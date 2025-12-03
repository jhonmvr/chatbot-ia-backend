package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.types.Channel;
import java.util.Optional;

public interface ConversationRepository {
    Optional<Conversation> findById(UuidId<Conversation> id);
    void save(Conversation conversation);
    
    /**
     * Busca una conversación abierta por cliente, contacto y canal
     * Útil para reutilizar conversaciones en lugar de crear una nueva cada vez
     */
    Optional<Conversation> findOpenByClientAndContactAndChannel(
            UuidId<Client> clientId,
            UuidId<Contact> contactId,
            Channel channel
    );
    
    /**
     * Busca todas las conversaciones abiertas
     * Útil para cerrar todas las conversaciones a las 12 de la noche
     */
    java.util.List<Conversation> findAllOpen();
    
    /**
     * Busca conversaciones abiertas sin mensajes desde una fecha específica
     * Útil para cerrar conversaciones inactivas
     */
    java.util.List<Conversation> findOpenInactiveSince(java.time.Instant since);
    
    /**
     * Búsqueda avanzada de conversaciones con paginación
     * Permite buscar en conversaciones, mensajes y contactos
     * 
     * @param clientId ID del cliente (opcional, si es null busca en todos)
     * @param query Texto de búsqueda (busca en títulos de conversación, contenido de mensajes, nombres de contactos)
     * @param contactId ID del contacto para filtrar (opcional)
     * @param status Estado de la conversación (opcional)
     * @param channel Canal de comunicación (opcional)
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @return Resultado de búsqueda con paginación
     */
    SearchResult searchConversations(
        UuidId<Client> clientId,
        String query,
        UuidId<Contact> contactId,
        String status,
        Channel channel,
        int page,
        int size
    );
    
    /**
     * Obtiene todas las conversaciones de un contacto específico
     * 
     * @param contactId ID del contacto
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @return Resultado con paginación
     */
    SearchResult findByContact(
        UuidId<Contact> contactId,
        int page,
        int size
    );
    
    /**
     * Resultado de búsqueda con paginación
     */
    record SearchResult(
        java.util.List<Conversation> conversations,
        long total,
        int page,
        int size,
        int totalPages
    ) {}
    
    /**
     * Obtiene contactos únicos que tengan conversaciones, ordenados por el mensaje más reciente
     * 
     * @param clientId ID del cliente (requerido)
     * @param query Texto de búsqueda (busca en nombre del contacto, teléfono y contenido de mensajes)
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @return Resultado con información del contacto, última conversación y último mensaje
     */
    ContactConversationResult findContactsWithConversations(
        UuidId<Client> clientId,
        String query,
        int page,
        int size
    );
    
    /**
     * Resultado de búsqueda de contactos con conversaciones
     */
    record ContactConversationResult(
        java.util.List<ContactConversationInfo> contacts,
        long total,
        int page,
        int size,
        int totalPages
    ) {}
    
    /**
     * Información de contacto con su última conversación y mensaje
     */
    record ContactConversationInfo(
        Contact contact,
        Conversation lastConversation,
        Message lastMessage
    ) {}
}
