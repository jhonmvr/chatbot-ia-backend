package com.relative.chat.bot.ia.application.usecases;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.types.EntityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso: Obtener o crear un contacto
 */
@Service
@RequiredArgsConstructor
public class GetOrCreateContact {
    
    private final ContactRepository contactRepository;
    
    /**
     * Obtiene un contacto existente o crea uno nuevo
     * 
     * @param clientId ID del cliente
     * @param phoneNumber Número de teléfono del contacto
     * @param displayName Nombre del contacto
     * @param channel Canal de comunicación
     * @return Contacto encontrado o creado
     */
    @Transactional
    public Contact handle(
            UuidId<Client> clientId,
            String phoneNumber,
            String displayName,
            Channel channel
    ) {
        // Buscar contacto existente por cliente y teléfono
        Optional<Contact> existingContact = contactRepository.findByClientAndPhone(clientId, phoneNumber);
        
        if (existingContact.isPresent()) {
            return existingContact.get();
        }
        
        // Crear nuevo contacto
        Contact newContact = new Contact(
                UuidId.newId(),
                clientId,
                displayName != null ? displayName : phoneNumber,
                null, // email
                null, // tags
                EntityStatus.ACTIVE
        );
        
        contactRepository.save(newContact);
        
        return newContact;
    }
}

