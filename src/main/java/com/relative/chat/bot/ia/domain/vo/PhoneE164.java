package com.relative.chat.bot.ia.domain.vo;
 import com.relative.chat.bot.ia.domain.common.DomainException;
 public record PhoneE164(String value){
public PhoneE164{
if(value==null || !value.startsWith("+") || value.length()<8 || value.length()>16) throw new DomainException("Teléfono E164 inválido");
 }
}