package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.adapters;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.domain.ports.messaging.MessageRepository;
import com.relative.chat.bot.ia.domain.types.Channel;
import com.relative.chat.bot.ia.domain.types.Direction;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.MessageEntity;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientPhoneEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ConversationEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.repositories.MessageJpa;

import jakarta.persistence.EntityManager;

import jakarta.persistence.PersistenceContext;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;


import java.time.ZoneOffset;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class MessageRepositoryAdapter implements MessageRepository {

  private final MessageJpa repo;

  @PersistenceContext private EntityManager em;


  private static Message toDomain(MessageEntity e){
    Message d = new Message(
        UuidId.of(e.getId()),
        UuidId.of(e.getClientEntity().getId()),
        UuidId.of(e.getConversationEntity().getId()),
        e.getContactEntity() != null ? UuidId.of(e.getContactEntity().getId()) : null,
        e.getPhone() != null ? UuidId.of(e.getPhone().getId()) : null,
        Channel.valueOf(e.getChannel()),
        Direction.valueOf(e.getDirection()),
        e.getBody(),
        e.getCreatedAt() == null ? null : e.getCreatedAt().toInstant()
    );

    if (e.getDeliveredAt() != null) d.markDelivered(e.getDeliveredAt().toInstant());
    if (e.getReadAt() != null) d.markRead(e.getReadAt().toInstant());

    return d;
  }

  @Override
  public void save(Message d) {
    MessageEntity e = new MessageEntity();

    e.setId(d.id().value());

    e.setClientEntity(em.getReference(ClientEntity.class, d.clientId().value()));

    e.setConversationEntity(em.getReference(ConversationEntity.class, d.conversationId().value()));

    d.contactId().ifPresent(cid -> e.setContactEntity(em.getReference(ContactEntity.class, cid.value())));

    d.phoneId().ifPresent(pid -> e.setPhone(em.getReference(ClientPhoneEntity.class, pid.value())));

    e.setChannel(d.channel().name());

    e.setDirection(d.direction().name());

    e.setBody(d.content());

    if (d.createdAt() != null) {
        e.setCreatedAt(d.createdAt().atOffset(ZoneOffset.UTC));
    } else {
        e.setCreatedAt(java.time.OffsetDateTime.now());
    }

    // Campos requeridos con defaults
    e.setMessageType("TEXT"); // default
    e.setMedia(new java.util.HashMap<>()); // default empty JSONB
    e.setStatus(d.status().name()); // usando el status del dominio

    //d.sentAt().ifPresent(ts -> e.setSentAt(ts.atOffset(ZoneOffset.UTC)));

    //d.deliveredAt().ifPresent(ts -> e.setDeliveredAt(ts.atOffset(ZoneOffset.UTC)));

    //d.readAt().ifPresent(ts -> e.setReadAt(ts.atOffset(ZoneOffset.UTC)));

    //d.externalId().ifPresent(e::setExternalId);

    //d.error().ifPresent(e::setError);

    repo.save(e);

  }

  @Override
  public List<Message> findByConversation(UuidId<Conversation> conversationId, int limit) {
    return repo.findTop100ByConversationEntityIdOrderByCreatedAtDesc(conversationId.value())
               .stream().limit(limit).map(MessageRepositoryAdapter::toDomain).toList();
  }
}
