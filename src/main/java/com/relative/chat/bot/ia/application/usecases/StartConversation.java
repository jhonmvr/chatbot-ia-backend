package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Caso de uso: Iniciar una nueva conversación
 */
@Service
@RequiredArgsConstructor
public class StartConversation {
    
    private final ConversationRepository conversationRepository;
    
    /**
     * Inicia una nueva conversación
     * 
     * @param clientId ID del cliente
     * @param contactId ID del contacto
     * @param phoneId ID del teléfono del cliente (opcional)
     * @param channel Canal de comunicación
     * @param title Título de la conversación
     * @return Conversación creada
     */
    @Transactional
    public Conversation handle(
            UuidId<Client> clientId,
            UuidId<Contact> contactId,
            UuidId<ClientPhone> phoneId,
            Channel channel,
            String title
    ) {
        Conversation conversation = new Conversation(
                UuidId.newId(),
                clientId,
                contactId,
                phoneId,
                channel,
                title != null ? title : "Conversación WhatsApp",
                Instant.now()
        );
        
        conversationRepository.save(conversation);
        
        return conversation;
    }
}

