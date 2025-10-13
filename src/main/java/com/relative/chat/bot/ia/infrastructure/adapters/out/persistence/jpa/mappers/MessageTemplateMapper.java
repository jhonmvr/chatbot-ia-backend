
package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;


import com.relative.chat.bot.ia.domain.messaging.MessageTemplate;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.MessageTemplateEntity;

import com.relative.chat.bot.ia.domain.types.*;


public final class MessageTemplateMapper {
  private MessageTemplateMapper(){}
  public static MessageTemplate toDomain(MessageTemplateEntity e){
    if(e==null) return null;

    return new MessageTemplate(
      MappingHelpers.toUuidId(e.getId()),
      e.getClientEntity()!=null?MappingHelpers.toUuidId(e.getClientEntity().getId()):null,
      null, e.getName(),
      e.getChannel()==null?Channel.WHATSAPP:Channel.valueOf(e.getChannel()),
            Language.ES,
      "",
            EntityStatus.ACTIVE
    );

  }
  public static MessageTemplateEntity toEntity(MessageTemplate d){
    if(d==null) return null;

    MessageTemplateEntity e = new MessageTemplateEntity();

    e.setId(MappingHelpers.toUuid(d.id()));

    //e.setCode(d.code());

    e.setName(d.name());

    e.setChannel(d.channel().name());

   // e.setLang(com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.enums.Language.valueOf(d.lang().name()));

    //e.setContent(d.content());

    //e.setStatus(com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.enums.EntityStatus.valueOf(d.status().name()));

    return e;

  }
}
