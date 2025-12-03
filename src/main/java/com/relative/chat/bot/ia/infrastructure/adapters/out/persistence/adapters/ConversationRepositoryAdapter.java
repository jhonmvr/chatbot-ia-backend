package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.ports.messaging.ConversationRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.types.Direction;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ConversationEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.MessageEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers.ContactMapper;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.ConversationJpa;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.MessageJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConversationRepositoryAdapter implements ConversationRepository {
    
    private final ConversationJpa repo;
    private final MessageJpa messageJpa;
    
    @PersistenceContext
    private EntityManager em;
    
    private static Conversation toDomain(ConversationEntity e) {
        return new Conversation(
            UuidId.of(e.getId()),
            UuidId.of(e.getClientEntity().getId()),
            e.getContactEntity() != null ? UuidId.of(e.getContactEntity().getId()) : null,
            e.getPhone() != null ? UuidId.of(e.getPhone().getId()) : null,
            Channel.valueOf(e.getChannel()),
            e.getTitle(),
            e.getStartedAt() == null ? null : e.getStartedAt().toInstant()
        );
    }
    
    @Override
    public Optional<Conversation> findById(UuidId<Conversation> id) {
        return repo.findById(id.value()).map(ConversationRepositoryAdapter::toDomain);
    }
    
    @Override
    public Optional<Conversation> findOpenByClientAndContactAndChannel(
            UuidId<com.relative.chat.bot.ia.domain.identity.Client> clientId,
            UuidId<com.relative.chat.bot.ia.domain.messaging.Contact> contactId,
            com.relative.chat.bot.ia.domain.types.Channel channel
    ) {
        return repo.findFirstOpenByClientAndContactAndChannel(
                clientId.value(),
                contactId.value(),
                channel.name()
        ).map(ConversationRepositoryAdapter::toDomain);
    }
    
    @Override
    public void save(Conversation d) {
        ConversationEntity e = new ConversationEntity();
        
        e.setId(d.id().value());
        e.setClientEntity(em.getReference(ClientEntity.class, d.clientId().value()));
        
        d.contactId().ifPresent(cid -> 
            e.setContactEntity(em.getReference(ContactEntity.class, cid.value()))
        );
        
        d.phoneId().ifPresent(pid -> 
            e.setPhone(em.getReference(ClientPhoneEntity.class, pid.value()))
        );
        
        e.setChannel(d.channel().name());
        e.setTitle(d.title());
        e.setStatus(d.status().name());
        
        if (d.startedAt() != null) {
            e.setStartedAt(d.startedAt().atOffset(ZoneOffset.UTC));
        } else {
            e.setStartedAt(java.time.OffsetDateTime.now());
        }
        
        d.closedAt().ifPresent(ts -> e.setClosedAt(ts.atOffset(ZoneOffset.UTC)));
        
        // Campos requeridos con timestamp
        e.setCreatedAt(java.time.OffsetDateTime.now());
        e.setUpdatedAt(java.time.OffsetDateTime.now());
        
        repo.save(e);
    }
    
    @Override
    public java.util.List<Conversation> findAllOpen() {
        return repo.findAllOpen().stream()
                .map(ConversationRepositoryAdapter::toDomain)
                .toList();
    }
    
    @Override
    public java.util.List<Conversation> findOpenInactiveSince(java.time.Instant since) {
        java.time.OffsetDateTime sinceOffset = since.atOffset(ZoneOffset.UTC);
        return repo.findOpenInactiveSince(sinceOffset).stream()
                .map(ConversationRepositoryAdapter::toDomain)
                .toList();
    }
    
    @Override
    public ConversationRepository.SearchResult searchConversations(
            UuidId<Client> clientId,
            String query,
            UuidId<Contact> contactId,
            String status,
            Channel channel,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        var result = repo.searchConversations(
            clientId != null ? clientId.value() : null,
            query,
            contactId != null ? contactId.value() : null,
            status,
            channel != null ? channel.name() : null,
            pageable
        );
        
        List<Conversation> conversations = result.getContent().stream()
                .map(ConversationRepositoryAdapter::toDomain)
                .toList();
        
        return new ConversationRepository.SearchResult(
            conversations,
            result.getTotalElements(),
            page,
            size,
            result.getTotalPages()
        );
    }
    
    @Override
    public ConversationRepository.SearchResult findByContact(
            UuidId<Contact> contactId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        var result = repo.findByContactId(contactId.value(), pageable);
        
        List<Conversation> conversations = result.getContent().stream()
                .map(ConversationRepositoryAdapter::toDomain)
                .toList();
        
        return new ConversationRepository.SearchResult(
            conversations,
            result.getTotalElements(),
            page,
            size,
            result.getTotalPages()
        );
    }
    
    @Override
    public ConversationRepository.ContactConversationResult findContactsWithConversations(
            UuidId<Client> clientId,
            String query,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        var result = repo.findContactsWithConversations(
            clientId.value(),
            query,
            pageable
        );
        
        List<ConversationRepository.ContactConversationInfo> contactInfos = new ArrayList<>();
        
        for (ContactEntity contactEntity : result.getContent()) {
            Contact contact = ContactMapper.toDomain(contactEntity);
            UuidId<Contact> contactId = UuidId.of(contactEntity.getId());
            
            // Obtener el último mensaje del contacto
            Optional<MessageEntity> lastMessageEntityOpt = messageJpa.findLastMessageByContact(contactId.value());
            
            Message lastMessage = null;
            Conversation lastConversation = null;
            
            if (lastMessageEntityOpt.isPresent()) {
                MessageEntity lastMessageEntity = lastMessageEntityOpt.get();
                
                // Convertir mensaje a dominio
                lastMessage = new Message(
                    UuidId.of(lastMessageEntity.getId()),
                    UuidId.of(lastMessageEntity.getClientEntity().getId()),
                    UuidId.of(lastMessageEntity.getConversationEntity().getId()),
                    lastMessageEntity.getContactEntity() != null ? UuidId.of(lastMessageEntity.getContactEntity().getId()) : null,
                    lastMessageEntity.getPhone() != null ? UuidId.of(lastMessageEntity.getPhone().getId()) : null,
                    Channel.valueOf(lastMessageEntity.getChannel()),
                    Direction.valueOf(lastMessageEntity.getDirection()),
                    lastMessageEntity.getBody(),
                    lastMessageEntity.getCreatedAt() != null ? lastMessageEntity.getCreatedAt().toInstant() : null
                );
                
                if (lastMessageEntity.getDeliveredAt() != null) {
                    lastMessage.markDelivered(lastMessageEntity.getDeliveredAt().toInstant());
                }
                if (lastMessageEntity.getReadAt() != null) {
                    lastMessage.markRead(lastMessageEntity.getReadAt().toInstant());
                }
                
                // Obtener la conversación del último mensaje
                ConversationEntity lastConvEntity = lastMessageEntity.getConversationEntity();
                lastConversation = toDomain(lastConvEntity);
            }
            
            contactInfos.add(new ConversationRepository.ContactConversationInfo(
                contact,
                lastConversation,
                lastMessage
            ));
        }
        
        return new ConversationRepository.ContactConversationResult(
            contactInfos,
            result.getTotalElements(),
            page,
            size,
            result.getTotalPages()
        );
    }
}
