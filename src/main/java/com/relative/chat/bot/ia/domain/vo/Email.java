package com.relative.chat.bot.ia.domain.vo;
 import com.relative.chat.bot.ia.domain.common.DomainException;
 import java.util.regex.*;
 public record Email(String value){
private static final Pattern P=Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
 public Email{
if(value!=null && !value.isBlank() && !P.matcher(value).matches()) throw new DomainException("Email inválido");
}}