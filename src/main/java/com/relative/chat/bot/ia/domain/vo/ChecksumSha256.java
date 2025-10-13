package com.relative.chat.bot.ia.domain.vo;
 import com.relative.chat.bot.ia.domain.common.DomainException;
 public record ChecksumSha256(String hex){
public ChecksumSha256{
if(hex!=null && !hex.matches("^[a-fA-F0-9]{64}$")) throw new DomainException("SHA-256 inválido");
 }
}