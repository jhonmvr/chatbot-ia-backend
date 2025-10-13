
package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;


import com.relative.chat.bot.ia.domain.messaging.Message;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.MessageEntity;

import com.relative.chat.bot.ia.domain.types.*;


import java.time.ZoneOffset;


public final class MessageMapper {
  private MessageMapper(){}
  public static Message toDomain(MessageEntity e){
    if(e==null) return null;

    Message d = new Message(
      MappingHelpers.toUuidId(e.getId()),
      e.getClientEntity()!=null?MappingHelpers.toUuidId(e.getClientEntity().getId()):null,
      e.getConversationEntity()!=null?MappingHelpers.toUuidId(e.getConversationEntity().getId()):null,
      e.getContactEntity()!=null?MappingHelpers.toUuidId(e.getContactEntity().getId()):null,
      e.getPhone()!=null?MappingHelpers.toUuidId(e.getPhone().getId()):null,
      e.getChannel()==null?Channel.WHATSAPP:Channel.valueOf(e.getChannel()),
      e.getDirection()==null?Direction.OUT:Direction.valueOf(e.getDirection()),
      e.getBody(),
      e.getCreatedAt()==null?null:e.getCreatedAt().toInstant()
    );

    if(e.getSentAt()!=null) {
        d.markSent(
            e.getSentAt().toInstant(), 
            e.getProvider() != null ? e.getProvider() : ""
        );
    }

    if(e.getDeliveredAt()!=null) d.markDelivered(e.getDeliveredAt().toInstant());

    if(e.getReadAt()!=null) d.markRead(e.getReadAt().toInstant());

    if(e.getErrorCode()!=null) d.fail(e.getErrorCode());

    return d;

  }
  public static MessageEntity toEntity(com.relative.chat.bot.ia.domain.messaging.Message d){
    if(d==null) return null;

    MessageEntity e = new MessageEntity();

    e.setId(MappingHelpers.toUuid(d.id()));

    e.setChannel(d.channel().name());

    e.setDirection(d.direction().name());

    e.setBody(d.content());

    if(d.createdAt()!=null) e.setCreatedAt(d.createdAt().atOffset(ZoneOffset.UTC));

    d.sentAt().ifPresent(ts -> e.setSentAt(ts.atOffset(ZoneOffset.UTC)));

    d.deliveredAt().ifPresent(ts -> e.setDeliveredAt(ts.atOffset(ZoneOffset.UTC)));

    d.readAt().ifPresent(ts -> e.setReadAt(ts.atOffset(ZoneOffset.UTC)));

    d.externalId().ifPresent(e::setProvider);

    d.error().ifPresent(e::setErrorCode);

    return e;

  }
}
