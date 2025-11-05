package com.relative.chat.bot.ia.application.usecases;

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
import com.relative.chat.bot.ia.infrastructure.adapters.out.whatsapp.WhatsAppProviderRouter;
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
    private final WhatsAppProviderRouter whatsAppRouter;
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
            if (channel == Channel.WHATSAPP && whatsAppRouter != null) {
                // Determinar el número de origen y el provider basándose en la configuración del cliente
                ProviderInfo providerInfo = determineProviderInfo(clientId, phoneId, fromNumber);
                
                String externalId = whatsAppRouter.sendMessage(
                    providerInfo.fromNumber(), 
                    toNumber, 
                    content, 
                    providerInfo.provider()
                );
                message.markSent(Instant.now(), externalId);
                log.info("Mensaje enviado exitosamente usando provider {}. ID externo: {}", 
                    providerInfo.provider(), externalId);
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
     * Información del provider y número de origen
     */
    private record ProviderInfo(String provider, String fromNumber) {}
    
    /**
     * Determina el provider y el número de origen para enviar el mensaje usando la nueva arquitectura parametrizable
     * 
     * @param clientId ID del cliente
     * @param phoneId ID del teléfono (opcional)
     * @param fromNumber Número de origen proporcionado (opcional)
     * @return Información del provider y número de origen
     */
    private ProviderInfo determineProviderInfo(UuidId<Client> clientId, UuidId<ClientPhone> phoneId, String fromNumber) {
        // Si se proporciona un phoneId específico, usarlo
        if (phoneId != null) {
            Optional<ClientPhone> phoneOpt = clientPhoneRepository.findById(phoneId);
            if (phoneOpt.isPresent()) {
                ClientPhone phone = phoneOpt.get();
                String provider = phone.provider() != null ? phone.provider() : "META";
                
                // Obtener configuración del proveedor usando la nueva arquitectura
                Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> configOpt = 
                        configServiceV2.getProviderConfiguration(phone.id(), provider);
                
                if (configOpt.isPresent()) {
                    WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configOpt.get();
                    
                    // Para Meta, usar el phone_number_id si está disponible
                    if ("META".equalsIgnoreCase(provider)) {
                        String phoneNumberId = config.getConfigValueOrDefault("phone_number_id", "");
                        if (!phoneNumberId.isEmpty()) {
                            return new ProviderInfo(provider, phoneNumberId);
                        }
                    }
                    
                    // Para otros proveedores, usar el provider_sid o el número E164
                    String actualFromNumber = phone.providerSidOpt().orElse(phone.phone().value());
                    return new ProviderInfo(provider, actualFromNumber);
                }
                
                // Fallback a la lógica anterior si no hay configuración parametrizable
                // Para Meta, usar el provider_sid como phone_number_id
                if ("META".equalsIgnoreCase(provider) && phone.providerSidOpt().isPresent()) {
                    return new ProviderInfo(provider, phone.providerSidOpt().get());
                }
                String actualFromNumber = phone.providerSidOpt().orElse(phone.phone().value());
                return new ProviderInfo(provider, actualFromNumber);
            }
        }
        
        // Si no hay phoneId, buscar el teléfono por defecto del cliente
        Optional<ProviderInfo> providerInfo = clientPhoneRepository.findByClient(clientId)
                .stream()
                .filter(phone -> phone.status() == com.relative.chat.bot.ia.domain.types.EntityStatus.ACTIVE)
                .findFirst()
                .map(phone -> {
                    String provider = phone.provider() != null ? phone.provider() : "META";
                    
                    // Obtener configuración del proveedor usando la nueva arquitectura
                    Optional<WhatsAppProviderConfigServiceV2.ProviderConfiguration> configOpt = 
                            configServiceV2.getProviderConfiguration(phone.id(), provider);
                    
                    if (configOpt.isPresent()) {
                        WhatsAppProviderConfigServiceV2.ProviderConfiguration config = configOpt.get();
                        
                        // Para Meta, usar el phone_number_id si está disponible
                        if ("META".equalsIgnoreCase(provider)) {
                            String phoneNumberId = config.getConfigValueOrDefault("phone_number_id", "");
                            if (!phoneNumberId.isEmpty()) {
                                return new ProviderInfo(provider, phoneNumberId);
                            }
                        }
                        
                        // Para otros proveedores, usar el provider_sid o el número E164
                        String actualFromNumber = phone.providerSidOpt().orElse(phone.phone().value());
                        return new ProviderInfo(provider, actualFromNumber);
                    }
                    
                    // Fallback a la lógica anterior si no hay configuración parametrizable
                    // Para Meta, usar el provider_sid como phone_number_id
                    if ("META".equalsIgnoreCase(provider) && phone.providerSidOpt().isPresent()) {
                        return new ProviderInfo(provider, phone.providerSidOpt().get());
                    }
                    String actualFromNumber = phone.providerSidOpt().orElse(phone.phone().value());
                    return new ProviderInfo(provider, actualFromNumber);
                });
        
        // Si no se encuentra un teléfono, usar el fromNumber proporcionado con provider por defecto
        if (providerInfo.isPresent()) {
            return providerInfo.get();
        }
        
        // Fallback: usar el número proporcionado con provider META por defecto
        log.warn("No se encontró teléfono activo. Usando fromNumber proporcionado con provider META por defecto");
        return new ProviderInfo("META", fromNumber);
    }
}

