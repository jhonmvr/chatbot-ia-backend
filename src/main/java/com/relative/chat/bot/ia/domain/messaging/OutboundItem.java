package com.relative.chat.bot.ia.domain.messaging;
 import com.relative.chat.bot.ia.domain.common.*;
 import com.relative.chat.bot.ia.domain.identity.*;
 import com.relative.chat.bot.ia.domain.types.*;
 import java.time.*;
 import java.util.*;
 public final class OutboundItem{
private final Id<OutboundItem> id;
 private final Id<Client> clientId;
 private final Id<Contact> contactId;
 private final Id<Conversation> conversationId;
 private final Id<ClientPhone> phoneId;
 private final Channel channel;
 private QueueStatus status;
 private String payload;
 private int attempts;
 private Instant nextAttemptAt;
 private String lastError;
 public OutboundItem(Id<OutboundItem> id, Id<Client> clientId, Id<Contact> contactId, Id<Conversation> conversationId, Id<ClientPhone> phoneId, Channel channel, String payload){
this.id=id;
    this.clientId=clientId;
     this.contactId = contactId;
     this.conversationId = conversationId;
     this.phoneId = phoneId;
     this.channel=channel;
    this.payload=payload;
    this.status=QueueStatus.PENDING;
    this.attempts=0;
 }

  public OutboundItem(Id<Object> objectId, Id<Object> of, Channel channel, String body, Id<OutboundItem> id, Id<Client> clientId, Id<Contact> contactId, Id<Conversation> conversationId, Id<ClientPhone> phoneId, Channel channel1) {
      this.id = id;
      this.clientId = clientId;
      this.contactId = contactId;
      this.conversationId = conversationId;
      this.phoneId = phoneId;
      this.channel = channel1;
  }

  public Id<OutboundItem> id(){return id;
}
public Id<Client> clientId(){return clientId;
}
public Optional<Id<Contact>> contactId(){return Optional.ofNullable(contactId);
}
public Optional<Id<Conversation>> conversationId(){return Optional.ofNullable(conversationId);
}
public Optional<Id<ClientPhone>> phoneId(){return Optional.ofNullable(phoneId);
}
public Channel channel(){return channel;
}
public QueueStatus status(){return status;
}
public String payload(){return payload;
}
public int attempts(){return attempts;
}
public Optional<Instant> nextAttemptAt(){return Optional.ofNullable(nextAttemptAt);
}
public Optional<String> lastError(){return Optional.ofNullable(lastError);
}
}