package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.ports.messaging.ContactRepository;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers.ContactMapper;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ContactJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    @Transactional
    public void save(Contact contact) {
        ContactEntity ent = ContactMapper.toEntity(contact, em);

        if (contact.id() != null) {
            // Si es una actualización, preservar las relaciones existentes
            ContactEntity existingEntity = contactJpa.findById(contact.id().value())
                    .orElseThrow(() -> new EntityNotFoundException("Contact not found"));

            // Preservar tags y categories existentes
            ent.setTags(existingEntity.getTags());
            ent.setCategories(existingEntity.getCategories());
        } else {
            // Si es una creación nueva, inicializar listas vacías
            ent.setTags(new ArrayList<>());
            ent.setCategories(new ArrayList<>());
        }

        contactJpa.save(ent);
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
    
    @Override
    public ContactRepository.SearchResult searchContacts(
            UuidId<Client> clientId,
            String query,
            Boolean isVip,
            Boolean isActive,
            String tag,
            int page,
            int size
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        
        var result = contactJpa.searchContacts(
            clientId != null ? clientId.value() : null,
            query,
            isVip,
            isActive,
            tag,
            pageable
        );
        
        List<Contact> contacts = result.getContent().stream()
                .map(ContactMapper::toDomain)
                .collect(Collectors.toList());
        
        return new ContactRepository.SearchResult(
            contacts,
            result.getTotalElements(),
            page,
            size,
            result.getTotalPages()
        );
    }
}

