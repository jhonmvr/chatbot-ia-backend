package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers.ContactMapper;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ContactJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter del repositorio de Contactos
 */
@Repository
@RequiredArgsConstructor
public class ContactRepositoryAdapter implements ContactRepository {
    
    private final ContactJpa contactJpa;
    
    @PersistenceContext
    private EntityManager em;
    
    @Override
    public Optional<Contact> findById(UuidId<Contact> id) {
        return contactJpa.findById(id.value())
                .map(ContactMapper::toDomain);
    }
    
    @Override
    public Optional<Contact> findByClientAndPhone(UuidId<Client> clientId, String phoneNumber) {
        return contactJpa.findByClientEntityIdAndPhoneE164(clientId.value(), phoneNumber)
                .map(ContactMapper::toDomain);
    }
    
    @Override
    public void save(Contact contact) {
        contactJpa.save(ContactMapper.toEntity(contact, em));
    }
    
    @Override
    public List<Contact> findByClient(UuidId<Client> clientId) {
        return contactJpa.findByClientEntityId(clientId.value()).stream()
                .map(ContactMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void delete(UuidId<Contact> id) {
        contactJpa.deleteById(id.value());
    }
    
    @Override
    public List<Contact> findAll() {
        return contactJpa.findAll().stream()
                .map(ContactMapper::toDomain)
                .collect(Collectors.toList());
    }
}

