package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.identity.*;
import com.relative.chat.bot.ia.domain.types.*;
import java.time.*;
import java.util.*;

public final class OutboundItem {
    
    private final LongId<OutboundItem> id;
    private final UuidId<Client> clientId;
    private final UuidId<Contact> contactId;
    private final UuidId<Conversation> conversationId;
    private final UuidId<ClientPhone> phoneId;
    private final Channel channel;
    private QueueStatus status;
    private String body;
    private int retries;
    private Instant scheduleAt;
    private String lastError;
    
    public OutboundItem(
            LongId<OutboundItem> id,
            UuidId<Client> clientId,
            UuidId<Contact> contactId,
            UuidId<Conversation> conversationId,
            UuidId<ClientPhone> phoneId,
            Channel channel,
            String body
    ) {
        this.id = id;
        this.clientId = clientId;
        this.contactId = contactId;
        this.conversationId = conversationId;
        this.phoneId = phoneId;
        this.channel = channel;
        this.body = body;
        this.status = QueueStatus.PENDING;
        this.retries = 0;
    }
    
    public void markSent() {
        this.status = QueueStatus.SENT;
    }
    
    public void markFailed(String error) {
        this.status = QueueStatus.FAILED;
        this.lastError = error;
        this.retries++;
    }
    
    public void scheduleRetry(Instant at) {
        this.status = QueueStatus.PENDING;
        this.scheduleAt = at;
        this.retries++;
    }
    
    public LongId<OutboundItem> id() {
        return id;
    }
    
    public UuidId<Client> clientId() {
        return clientId;
    }
    
    public Optional<UuidId<Contact>> contactId() {
        return Optional.ofNullable(contactId);
    }
    
    public Optional<UuidId<Conversation>> conversationId() {
        return Optional.ofNullable(conversationId);
    }
    
    public Optional<UuidId<ClientPhone>> phoneId() {
        return Optional.ofNullable(phoneId);
    }
    
    public Channel channel() {
        return channel;
    }
    
    public QueueStatus status() {
        return status;
    }
    
    public String body() {
        return body;
    }
    
    public int retries() {
        return retries;
    }
    
    public Optional<Instant> scheduleAt() {
        return Optional.ofNullable(scheduleAt);
    }
    
    public Optional<String> lastError() {
        return Optional.ofNullable(lastError);
    }
}