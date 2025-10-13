package com.relative.chat.bot.ia.domain.identity;

import com.relative.chat.bot.ia.domain.common.LongId;
import com.relative.chat.bot.ia.domain.common.UuidId;
import java.time.*;

public record UsageDaily(
        LongId<UsageDaily> id,
        UuidId<Client> clientId,
        LocalDate day,
        long messagesIn,
        long messagesOut,
        long tokensIn,
        long tokensOut
) {}