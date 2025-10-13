
package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;


import com.relative.chat.bot.ia.domain.identity.Client;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;

import com.relative.chat.bot.ia.domain.types.EntityStatus;


public final class ClientMapper {
  private ClientMapper(){}
  public static Client toDomain(ClientEntity e){
    if(e==null) return null;

    return new Client(
      MappingHelpers.toUuidId(e.getId()),
      e.getTaxId() != null ? e.getTaxId() : e.getId().toString().substring(0, 8),
      e.getName(),
      e.getStatus()==null?EntityStatus.ACTIVE:EntityStatus.valueOf(e.getStatus())
    );

  }
  public static ClientEntity toEntity(com.relative.chat.bot.ia.domain.identity.Client d){
    if(d==null) return null;

    ClientEntity e = new ClientEntity();

    e.setId(MappingHelpers.toUuid(d.id()));

    e.setTaxId(d.code());

    e.setName(d.name());

    e.setStatus(d.status().name());

    return e;

  }
}
