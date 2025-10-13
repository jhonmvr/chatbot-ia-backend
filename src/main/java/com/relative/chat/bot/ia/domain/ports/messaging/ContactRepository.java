package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;

import java.util.Optional;

/**
 * Puerto de repositorio para Contactos
 */
public interface ContactRepository {
    
    /**
     * Busca un contacto por ID
     */
    Optional<Contact> findById(UuidId<Contact> id);
    
    /**
     * Busca un contacto por cliente y número de teléfono
     */
    Optional<Contact> findByClientAndPhone(UuidId<Client> clientId, String phoneNumber);
    
    /**
     * Guarda un contacto
     */
    void save(Contact contact);
    
    /**
     * Busca contactos por cliente
     */
    java.util.List<Contact> findByClient(UuidId<Client> clientId);
}

