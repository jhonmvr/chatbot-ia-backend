package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.identity.*;
import com.relative.chat.bot.ia.domain.types.*;
import java.time.*;
import java.util.*;

public final class Message {
    
    private final UuidId<Message> id;
    private final UuidId<Client> clientId;
    private final UuidId<Conversation> conversationId;
    private final UuidId<Contact> contactId;
    private final UuidId<ClientPhone> phoneId;
    private final Channel channel;
    private final Direction direction;
    private final String content;
    private final Instant createdAt;
    private Instant sentAt, deliveredAt, readAt;
    private String externalId, error;
    
    public Message(
            UuidId<Message> id,
            UuidId<Client> clientId,
            UuidId<Conversation> conversationId,
            UuidId<Contact> contactId,
            UuidId<ClientPhone> phoneId,
            Channel channel,
            Direction direction,
            String content,
            Instant createdAt
    ) {
        this.id = id;
        this.clientId = clientId;
        this.conversationId = conversationId;
        this.contactId = contactId;
        this.phoneId = phoneId;
        this.channel = channel;
        this.direction = direction;
        this.content = content;
        this.createdAt = createdAt;
    }
    
    public void markSent(Instant at, String ext) {
        this.sentAt = at;
        this.externalId = ext;
    }
    
    public void markDelivered(Instant at) {
        this.deliveredAt = at;
    }
    
    public void markRead(Instant at) {
        this.readAt = at;
    }
    
    public void fail(String err) {
        this.error = err;
    }
    
    public UuidId<Message> id() {
        return id;
    }
    
    public UuidId<Client> clientId() {
        return clientId;
    }
    
    public UuidId<Conversation> conversationId() {
        return conversationId;
    }
    
    public Optional<UuidId<Contact>> contactId() {
        return Optional.ofNullable(contactId);
    }
    
    public Optional<UuidId<ClientPhone>> phoneId() {
        return Optional.ofNullable(phoneId);
    }
    
    public Channel channel() {
        return channel;
    }
    
    public Direction direction() {
        return direction;
    }
    
    public String content() {
        return content;
    }
    
    public Instant createdAt() {
        return createdAt;
    }
    
    public Optional<Instant> sentAt() {
        return Optional.ofNullable(sentAt);
    }
    
    public Optional<Instant> deliveredAt() {
        return Optional.ofNullable(deliveredAt);
    }
    
    public Optional<Instant> readAt() {
        return Optional.ofNullable(readAt);
    }
    
    public Optional<String> externalId() {
        return Optional.ofNullable(externalId);
    }
    
    public Optional<String> error() {
        return Optional.ofNullable(error);
    }
    
    public MessageStatus status() {
        if (error != null) return MessageStatus.FAILED;
        if (readAt != null) return MessageStatus.READ;
        if (deliveredAt != null) return MessageStatus.DELIVERED;
        if (sentAt != null) return MessageStatus.SENT;
        return MessageStatus.PENDING;
    }
}