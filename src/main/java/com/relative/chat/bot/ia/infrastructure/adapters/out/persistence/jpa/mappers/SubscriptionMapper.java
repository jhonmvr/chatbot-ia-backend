
package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;


import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.SubscriptionEntity;


public final class SubscriptionMapper {
  private SubscriptionMapper(){}
  public static com.relative.chat.bot.ia.domain.identity.Subscription toDomain(SubscriptionEntity e){
    if(e==null) return null;

    com.relative.chat.bot.ia.domain.identity.Subscription d = new com.relative.chat.bot.ia.domain.identity.Subscription(
      MappingHelpers.toUuidId(e.getId()),
      e.getClientEntity()!=null?MappingHelpers.toUuidId(e.getClientEntity().getId()):null,
      e.getPlanEntity()!=null?e.getPlanEntity().getCode():null,
      e.getStartDate()
    );

    if(e.getCancelAt()!=null) d.cancel(e.getCancelAt().toLocalDate());

    return d;

  }
  public static SubscriptionEntity toEntity(com.relative.chat.bot.ia.domain.identity.Subscription d){
    if(d==null) return null;

    SubscriptionEntity e = new SubscriptionEntity();

    e.setId(MappingHelpers.toUuid(d.id()));

    e.setStartDate(d.startDate());

    e.setStatus(d.status().name());

    return e;

  }
}
