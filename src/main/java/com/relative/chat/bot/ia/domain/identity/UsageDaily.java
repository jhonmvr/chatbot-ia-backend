package com.relative.chat.bot.ia.domain.identity;
 import com.relative.chat.bot.ia.domain.common.Id;
 import java.time.*;
 public record UsageDaily(Id<Client> clientId, LocalDate day, long messagesCount, long tokensIn, long tokensOut){}