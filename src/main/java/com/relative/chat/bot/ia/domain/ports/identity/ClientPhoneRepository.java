package com.relative.chat.bot.ia.domain.ports.identity;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.ClientPhone;
import com.relative.chat.bot.ia.domain.types.Channel;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para ClientPhone (números de WhatsApp del cliente)
 */
public interface ClientPhoneRepository {
    
    /**
     * Busca un teléfono del cliente por ID
     */
    Optional<ClientPhone> findById(UuidId<ClientPhone> id);
    
    /**
     * Busca todos los teléfonos de un cliente
     */
    List<ClientPhone> findByClient(UuidId<Client> clientId);
    
    /**
     * Busca el cliente por provider SID (phone_number_id de Meta, o SID de Twilio)
     * ESTE ES EL MÉTODO CLAVE PARA RESOLVER EL CLIENTE DESDE EL WEBHOOK
     * 
     * @param providerSid El identificador del proveedor (ej: phone_number_id de Meta)
     * @param provider El proveedor (META, TWILIO, etc)
     * @return El cliente dueño de ese número
     */
    Optional<Client> findClientByProviderSid(String providerSid, String provider);
    
    /**
     * Busca el ClientPhone por provider SID
     * 
     * @param providerSid El identificador del proveedor
     * @param provider El proveedor
     * @return El ClientPhone encontrado
     */
    Optional<ClientPhone> findByProviderSid(String providerSid, String provider);
    
    /**
     * Busca por número E164 y canal
     */
    Optional<ClientPhone> findByPhoneAndChannel(String e164, Channel channel);
    
    /**
     * Guarda un ClientPhone
     */
    void save(ClientPhone clientPhone);

    /**
     * Obtiene todos los registros
     */
    List<ClientPhone> findAll();
}

