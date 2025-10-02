package com.relative.chat.bot.ia.domain.messaging;
 import com.relative.chat.bot.ia.domain.common.*;
 import com.relative.chat.bot.ia.domain.identity.*;
 import com.relative.chat.bot.ia.domain.types.*;
 import java.time.*;
 import java.util.*;
 public final class Conversation{
private final Id<Conversation> id;
 private final Id<Client> clientId;
 private final Id<Contact> contactId;
 private final Id<ClientPhone> phoneId;
 private final Channel channel;
 private ConversationStatus status;
 private String title;
 private final Instant startedAt;
 private Instant closedAt;
 public Conversation(Id<Conversation> id, Id<Client> clientId, Id<Contact> contactId, Id<ClientPhone> phoneId, Channel channel, String title, Instant startedAt){
this.id=id;
 this.clientId=clientId;
 this.contactId=contactId;
 this.phoneId=phoneId;
 this.channel=channel;
 this.title=title;
 this.status=ConversationStatus.OPEN;
 this.startedAt=startedAt;
 }
public void close(Instant at){
if(status!=ConversationStatus.OPEN) throw new DomainException("Conversación no está abierta");
 status=ConversationStatus.CLOSED;
 closedAt=at;
 }
public Id<Conversation> id(){return id;
}
public Id<Client> clientId(){return clientId;
}
public Optional<Id<Contact>> contactId(){return Optional.ofNullable(contactId);
}
public Optional<Id<ClientPhone>> phoneId(){return Optional.ofNullable(phoneId);
}
public Channel channel(){return channel;
}
public ConversationStatus status(){return status;
}
public String title(){return title;
}
public Instant startedAt(){return startedAt;
}
public Optional<Instant> closedAt(){return Optional.ofNullable(closedAt);
}
}