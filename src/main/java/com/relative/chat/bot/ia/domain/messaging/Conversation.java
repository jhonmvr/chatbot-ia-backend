package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.identity.*;
import com.relative.chat.bot.ia.domain.types.*;
import java.time.*;
import java.util.*;

public final class Conversation {
    
    private final UuidId<Conversation> id;
    private final UuidId<Client> clientId;
    private final UuidId<Contact> contactId;
    private final UuidId<ClientPhone> phoneId;
    private final Channel channel;
    private ConversationStatus status;
    private String title;
    private final Instant startedAt;
    private Instant closedAt;
    
    public Conversation(
            UuidId<Conversation> id,
            UuidId<Client> clientId,
            UuidId<Contact> contactId,
            UuidId<ClientPhone> phoneId,
            Channel channel,
            String title,
            Instant startedAt
    ) {
        this.id = id;
        this.clientId = clientId;
        this.contactId = contactId;
        this.phoneId = phoneId;
        this.channel = channel;
        this.title = title;
        this.status = ConversationStatus.OPEN;
        this.startedAt = startedAt;
    }
    
    public void close(Instant at) {
        if (status != ConversationStatus.OPEN) {
            throw new DomainException("Conversación no está abierta");
        }
        status = ConversationStatus.CLOSED;
        closedAt = at;
    }
    
    public UuidId<Conversation> id() {
        return id;
    }
    
    public UuidId<Client> clientId() {
        return clientId;
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
    
    public ConversationStatus status() {
        return status;
    }
    
    public String title() {
        return title;
    }
    
    public Instant startedAt() {
        return startedAt;
    }
    
    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
    }
}