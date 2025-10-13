
package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;


import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ConversationEntity;

import com.relative.chat.bot.ia.domain.types.Channel;


import java.time.ZoneOffset;


public final class ConversationMapper {
  private ConversationMapper(){}
  public static com.relative.chat.bot.ia.domain.messaging.Conversation toDomain(ConversationEntity e){
    if(e==null) return null;

    return new com.relative.chat.bot.ia.domain.messaging.Conversation(
      MappingHelpers.toUuidId(e.getId()),
      e.getClientEntity()!=null?MappingHelpers.toUuidId(e.getClientEntity().getId()):null,
      e.getContactEntity()!=null?MappingHelpers.toUuidId(e.getContactEntity().getId()):null,
      e.getPhone()!=null?MappingHelpers.toUuidId(e.getPhone().getId()):null,
      e.getChannel()==null?Channel.WHATSAPP:Channel.valueOf(e.getChannel()),
      e.getTitle(),
      e.getStartedAt()==null?null:e.getStartedAt().toInstant()
    );

  }
  public static ConversationEntity toEntity(com.relative.chat.bot.ia.domain.messaging.Conversation d){
    if(d==null) return null;

    ConversationEntity e = new ConversationEntity();

    e.setId(MappingHelpers.toUuid(d.id()));

    e.setChannel(d.channel().name());

    e.setTitle(d.title());

    e.setStatus(d.status().name());

    if(d.startedAt()!=null) e.setStartedAt(d.startedAt().atOffset(ZoneOffset.UTC));

    d.closedAt().ifPresent(ts -> e.setClosedAt(ts.atOffset(ZoneOffset.UTC)));

    return e;

  }
}
