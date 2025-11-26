package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
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
}
