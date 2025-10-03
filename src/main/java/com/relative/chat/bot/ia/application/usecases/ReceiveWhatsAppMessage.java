package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.dto.MessageCommand;
import com.relative.chat.bot.ia.application.dto.MessageResponse;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.ports.identity.ClientRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.MessageRepository;
import com.relative.chat.bot.ia.domain.types.Direction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso principal: Recibir y procesar mensaje de WhatsApp
 * Orquesta todo el flujo desde recibir el mensaje hasta enviar la respuesta
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiveWhatsAppMessage {
    
    private final ClientRepository clientRepository;
    private final MessageRepository messageRepository;
    private final GetOrCreateContact getOrCreateContact;
    private final StartConversation startConversation;
    private final ProcessMessageWithAI processMessageWithAI;
    private final SendMessage sendMessage;
    
    /**
     * Procesa un mensaje entrante de WhatsApp
     * 
     * 1. Valida el cliente
     * 2. Obtiene o crea el contacto
     * 3. Obtiene o crea la conversación
     * 4. Guarda el mensaje entrante
     * 5. Genera respuesta con IA
     * 6. Envía la respuesta
     * 
     * @param command Comando con los datos del mensaje
     * @return Respuesta con el resultado del procesamiento
     */
    @Transactional
    public MessageResponse handle(MessageCommand command) {
        try {
            log.info("Recibiendo mensaje de WhatsApp: cliente={}, de={}, contenido={}",
                    command.clientCode(), command.contactPhone(), command.content());
            
            // 1. Obtener cliente
            Client client = getClient(command.clientCode());
            if (client == null) {
                return MessageResponse.error("Cliente no encontrado: " + command.clientCode());
            }
            
            // 2. Obtener o crear contacto
            Contact contact = getOrCreateContact.handle(
                    client.id(),
                    command.contactPhone(),
                    command.contactName(),
                    command.channel()
            );
            
            // 3. Obtener o crear conversación
            // TODO: Buscar conversación abierta existente
            Conversation conversation = startConversation.handle(
                    client.id(),
                    contact.id(),
                    null, // phoneId - obtener del comando si está disponible
                    command.channel(),
                    "Conversación con " + (command.contactName() != null ? command.contactName() : command.contactPhone())
            );
            
            // 4. Guardar mensaje entrante
            Message incomingMessage = createIncomingMessage(command, client, conversation, contact);
            messageRepository.save(incomingMessage);
            
            log.info("Mensaje entrante guardado: id={}", incomingMessage.id().value());
            
            // 5. Generar respuesta con IA
            String aiResponse = processMessageWithAI.handle(
                    command.content(),
                    conversation.id(),
                    "kb" // namespace del knowledge base - debería venir de configuración
            );
            
            // 6. Enviar respuesta
            if (aiResponse != null && !aiResponse.isBlank()) {
                Message responseMessage = sendMessage.handle(
                        client.id(),
                        conversation.id(),
                        contact.id(),
                        null, // phoneId
                        command.channel(),
                        aiResponse,
                        command.phoneNumber(),
                        command.contactPhone()
                );
                
                log.info("Respuesta enviada: id={}", responseMessage.id().value());
                
                return MessageResponse.success(
                        responseMessage.id().value(),
                        conversation.id().value(),
                        aiResponse
                );
            } else {
                return MessageResponse.error("No se pudo generar una respuesta");
            }
            
        } catch (Exception e) {
            log.error("Error al procesar mensaje de WhatsApp: {}", e.getMessage(), e);
            return MessageResponse.error("Error al procesar mensaje: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene el cliente por código
     */
    private Client getClient(String clientCode) {
        Optional<Client> clientOpt = clientRepository.findByCode(clientCode);
        return clientOpt.orElse(null);
    }
    
    /**
     * Crea el mensaje entrante
     */
    private Message createIncomingMessage(
            MessageCommand command,
            Client client,
            Conversation conversation,
            Contact contact
    ) {
        Message message = new Message(
                UuidId.newId(),
                client.id(),
                conversation.id(),
                contact.id(),
                null, // phoneId
                command.channel(),
                Direction.IN,
                command.content(),
                command.receivedAt()
        );
        
        // Si hay un ID externo del proveedor, marcarlo como recibido
        if (command.externalId() != null) {
            message.markSent(command.receivedAt(), command.externalId());
        }
        
        return message;
    }
}

