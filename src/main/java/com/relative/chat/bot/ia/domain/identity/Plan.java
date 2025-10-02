package com.relative.chat.bot.ia.domain.identity;

 import com.relative.chat.bot.ia.domain.vo.*;

 public record Plan(String code,String name,Money monthlyPrice,Integer msgLimitMonth,Integer usersLimit,Integer retentionDays,String aiModel){
 }
 