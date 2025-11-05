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
    
    /**
     * Busca contactos por cliente (alias para compatibilidad)
     */
    default java.util.List<Contact> findByClientId(UuidId<Client> clientId) {
        return findByClient(clientId);
    }
    
    /**
     * Elimina un contacto
     */
    void delete(UuidId<Contact> id);
    
    /**
     * Obtiene todos los contactos
     */
    java.util.List<Contact> findAll();
    
    /**
     * Búsqueda avanzada de contactos con paginación
     */
    SearchResult searchContacts(
        UuidId<Client> clientId,
        String query,
        Boolean isVip,
        Boolean isActive,
        String tag,
        int page,
        int size
    );
    
    /**
     * Resultado de búsqueda con paginación
     */
    record SearchResult(
        java.util.List<Contact> contacts,
        long total,
        int page,
        int size,
        int totalPages
    ) {}
}
