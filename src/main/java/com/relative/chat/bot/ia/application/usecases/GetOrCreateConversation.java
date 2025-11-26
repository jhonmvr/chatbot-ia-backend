package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Caso de uso: Obtener o crear conversación
 * Busca primero una conversación abierta existente, si no existe, crea una nueva
 * Esto es crítico para mantener el estado del flujo de agendamiento
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetOrCreateConversation {
    
    private final ConversationRepository conversationRepository;
    
    /**
     * Obtiene una conversación abierta existente o crea una nueva
     * 
     * @param clientId ID del cliente
     * @param contactId ID del contacto
     * @param phoneId ID del teléfono del cliente (opcional)
     * @param channel Canal de comunicación
     * @param title Título de la conversación
     * @return Conversación existente o nueva
     */
    @Transactional
    public Conversation handle(
            UuidId<Client> clientId,
            UuidId<Contact> contactId,
            UuidId<ClientPhone> phoneId,
            Channel channel,
            String title
    ) {
        // 1. Buscar conversación abierta existente
        Optional<Conversation> existingOpt = conversationRepository.findOpenByClientAndContactAndChannel(
                clientId,
                contactId,
                channel
        );
        
        if (existingOpt.isPresent()) {
            Conversation existing = existingOpt.get();
            log.info("✅ Reutilizando conversación existente: {} (status: {})", 
                    existing.id().value(), existing.status());
            return existing;
        }
        
        // 2. Crear nueva conversación si no existe
        log.info("📝 Creando nueva conversación para cliente: {}, contacto: {}", 
                clientId.value(), contactId.value());
        
        Conversation newConversation = new Conversation(
                UuidId.newId(),
                clientId,
                contactId,
                phoneId,
                channel,
                title != null ? title : "Conversación con " + contactId.value(),
                Instant.now()
        );
        
        conversationRepository.save(newConversation);
        
        log.info("✅ Nueva conversación creada: {}", newConversation.id().value());
        
        return newConversation;
    }
}

