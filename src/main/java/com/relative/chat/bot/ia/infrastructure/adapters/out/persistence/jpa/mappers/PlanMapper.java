
package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;


import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.PlanEntity;

import com.relative.chat.bot.ia.domain.vo.Money;


public final class PlanMapper {
  private PlanMapper(){}
  public static PlanEntity toEntity(com.relative.chat.bot.ia.domain.identity.Plan d){
    if(d==null) return null;

    PlanEntity e = new PlanEntity();

    e.setCode(d.code());

    e.setName(d.name());

    e.setMonthlyPriceUsd(MappingHelpers.moneyAmount(d.monthlyPrice()));

    e.setCurrency(MappingHelpers.moneyCurrency(d.monthlyPrice()));

    e.setMsgLimitMonth(Long.valueOf(d.msgLimitMonth()));

    e.setUsersLimit(d.usersLimit());

    e.setRetentionDays(d.retentionDays());

    e.setAiModel(d.aiModel());

    return e;

  }
  public static com.relative.chat.bot.ia.domain.identity.Plan toDomain(PlanEntity e){
    if(e==null) return null;

    Money price = MappingHelpers.money(e.getMonthlyPriceUsd(), e.getCurrency());

    return new com.relative.chat.bot.ia.domain.identity.Plan(e.getCode(), e.getName(), price,
            Math.toIntExact(e.getMsgLimitMonth()), e.getUsersLimit(), e.getRetentionDays(), e.getAiModel());

  }
}
