package com.relative.chat.bot.ia.domain.vo;
 import com.relative.chat.bot.ia.domain.common.DomainException;
 public record Tokens(int value){
public Tokens{
if(value<0) throw new DomainException("Tokens negativos");
 }
public Tokens add(Tokens o){
return new Tokens(this.value+o.value());
 }
}