package com.relative.chat.bot.ia.domain.messaging;
 import com.relative.chat.bot.ia.domain.common.*;
 import com.relative.chat.bot.ia.domain.identity.*;
 import com.relative.chat.bot.ia.domain.types.*;
 import java.time.*;
 import java.util.*;
 public final class Message{
private final Id<Message> id;
 private final Id<Client> clientId;
 private final Id<Conversation> conversationId;
 private final Id<Contact> contactId;
 private final Id<ClientPhone> phoneId;
 private final Channel channel;
 private final Direction direction;
 private final String content;
 private final Instant createdAt;
 private Instant sentAt, deliveredAt, readAt;
 private String externalId, error;
 public Message(Id<Message> id, Id<Client> clientId, Id<Conversation> conversationId, Id<Contact> contactId, Id<ClientPhone> phoneId, Channel channel, Direction direction, String content, Instant createdAt){
this.id=id;
 this.clientId=clientId;
 this.conversationId=conversationId;
 this.contactId=contactId;
 this.phoneId=phoneId;
 this.channel=channel;
 this.direction=direction;
 this.content=content;
 this.createdAt=createdAt;
 }
public void markSent(Instant at, String ext){
this.sentAt=at;
 this.externalId=ext;
 }
public void markDelivered(Instant at){
this.deliveredAt=at;
 }
public void markRead(Instant at){
this.readAt=at;
 }
public void fail(String err){
this.error=err;
 }
public Id<Message> id(){return id;
}
public Id<Client> clientId(){return clientId;
}
public Id<Conversation> conversationId(){return conversationId;
}
public Optional<Id<Contact>> contactId(){return Optional.ofNullable(contactId);
}
public Optional<Id<ClientPhone>> phoneId(){return Optional.ofNullable(phoneId);
}
public Channel channel(){return channel;
}
public Direction direction(){return direction;
}
public String content(){return content;
}
public Instant createdAt(){return createdAt;
}
}