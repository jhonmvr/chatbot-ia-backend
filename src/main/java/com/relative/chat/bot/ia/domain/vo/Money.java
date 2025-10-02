package com.relative.chat.bot.ia.domain.vo;
 import com.relative.chat.bot.ia.domain.common.DomainException;
 import java.math.*;
 import java.util.*;
 public record Money(BigDecimal amount,String currency){
public Money{
Objects.requireNonNull(amount);
 Objects.requireNonNull(currency);
 if(amount.scale()>2) throw new DomainException("Money: máximo 2 decimales");
 if(amount.compareTo(BigDecimal.ZERO)<0) throw new DomainException("Money: negativo");
 if(currency.isBlank()) throw new DomainException("Currency vacía");
 }
public Money add(Money o){
if(!currency.equals(o.currency)) throw new DomainException("Currency mismatch");
 return new Money(amount.add(o.amount), currency);
 }}