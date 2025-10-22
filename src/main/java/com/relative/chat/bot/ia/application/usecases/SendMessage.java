package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.application.ports.out.WhatsAppService;
import com.relative.chat.bot.ia.application.services.WhatsAppProviderConfigServiceV2;
import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.ports.identity.ClientPhoneRepository;
import com.relative.chat.bot.ia.domain.ports.messaging.MessageRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.types.Direction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Caso de uso: Enviar mensaje
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendMessage {
    
    private final MessageRepository messageRepository;
    private final WhatsAppService whatsAppService;
    private final ClientPhoneRepository clientPhoneRepository;
    private final WhatsAppProviderConfigServiceV2 configServiceV2;
    
    /**
     * Envía un mensaje al contacto
     * 
     * @param clientId ID del cliente
     * @param conversationId ID de la conversación
     * @param contactId ID del contacto
     * @param phoneId ID del teléfono del cliente
     * @param channel Canal de comunicación
     * @param content Contenido del mensaje
     * @param fromNumber Número de origen
     * @param toNumber Número de destino
     * @return Mensaje enviado
     */
    @Transactional
    public Message handle(
            UuidId<Client> clientId,
            UuidId<Conversation> conversationId,
            UuidId<Contact> contactId,
            UuidId<ClientPhone> phoneId,
            Channel channel,
            String content,
            String fromNumber,
            String toNumber
    ) {
        // Crear el mensaje
        Message message = new Message(
                UuidId.newId(),
                clientId,
                conversationId,
                contactId,
                phoneId,
                channel,
                Direction.OUT,
                content,
                Instant.now()
        );
        
        try {
            // Enviar a través del servicio externo
            if (channel == Channel.WHATSAPP && whatsAppService != null) {
                // Determinar el número de origen basándose en la configuración del cliente
                String actualFromNumber = determineFromNumber(clientId, phoneId, fromNumber);
                
                String externalId = whatsAppService.sendMessage(actualFromNumber, toNumber, content);
                message.markSent(Instant.now(), externalId);
                log.info("Mensaje enviado exitosamente. ID externo: {}", externalId);
            }
        } catch (Exception e) {
            log.error("Error al enviar mensaje: {}", e.getMessage(), e);
            message.fail(e.getMessage());
        }
        
        // Guardar el mensaje
        messageRepository.save(message);
        
        return message;
    }
    
    /**
     * Determina el número de origen para enviar el mensaje usando la nueva arquitectura parametrizable
     * 
     * @param clientId ID del cliente
     * @param phoneId ID del teléfono (opcional)
     * @param fromNumber Número de origen proporcionado (opcional)
     * @return Número de origen a usar
     */
    private String determineFromNumber(UuidId<Client> clientId, UuidId<ClientPhone> phoneId, String fromNumber) {
        // Si se proporciona un phoneId específico, usarlo
        if (phoneId != null) {
            Optional<ClientPhone> phoneOpt = clientPhoneRepository.findById(phoneId);
            if (phoneOpt.isPresent()) {
                ClientPhone phone = phoneOpt.get();
                
                // Obtener configuración del proveedor usando la nueva arquitectura
                Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> configOpt = 
                        configServiceV2.getProviderConfiguration(phone.id(), phone.provider());
                
                if (configOpt.isPresent()) {
                    WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configOpt.get();
                    
                    // Para Meta, usar el phone_number_id si está disponible
                    if ("META".equalsIgnoreCase(phone.provider())) {
                        String phoneNumberId = config.getConfigValueOrDefault("phone_number_id", "");
                        if (!phoneNumberId.isEmpty()) {
                            return phoneNumberId;
                        }
                    }
                    
                    // Para otros proveedores, usar el provider_sid o el número E164
                    return phone.providerSidOpt().orElse(phone.phone().value());
                }
                
                // Fallback a la lógica anterior si no hay configuración parametrizable
                // Para Meta, usar el provider_sid como phone_number_id
                if ("META".equalsIgnoreCase(phone.provider()) && phone.providerSidOpt().isPresent()) {
                    return phone.providerSidOpt().get();
                }
                return phone.providerSidOpt().orElse(phone.phone().value());
            }
        }
        
        // Si no hay phoneId, buscar el teléfono por defecto del cliente
        return clientPhoneRepository.findByClient(clientId)
                .stream()
                .filter(phone -> phone.status() == com.relative.chat.bot.ia.domain.types.EntityStatus.ACTIVE)
                .findFirst()
                .map(phone -> {
                    // Obtener configuración del proveedor usando la nueva arquitectura
                    Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> configOpt = 
                            configServiceV2.getProviderConfiguration(phone.id(), phone.provider());
                    
                    if (configOpt.isPresent()) {
                        WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configOpt.get();
                        
                        // Para Meta, usar el phone_number_id si está disponible
                        if ("META".equalsIgnoreCase(phone.provider())) {
                            String phoneNumberId = config.getConfigValueOrDefault("phone_number_id", "");
                            if (!phoneNumberId.isEmpty()) {
                                return phoneNumberId;
                            }
                        }
                        
                        // Para otros proveedores, usar el provider_sid o el número E164
                        return phone.providerSidOpt().orElse(phone.phone().value());
                    }
                    
                    // Fallback a la lógica anterior si no hay configuración parametrizable
                    // Para Meta, usar el provider_sid como phone_number_id
                    if ("META".equalsIgnoreCase(phone.provider()) && phone.providerSidOpt().isPresent()) {
                        return phone.providerSidOpt().get();
                    }
                    return phone.providerSidOpt().orElse(phone.phone().value());
                })
                .orElse(fromNumber); // Fallback al número proporcionado
    }
}

