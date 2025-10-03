package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.identity.*;
import com.relative.chat.bot.ia.domain.types.*;

public record MessageTemplate(
        UuidId<MessageTemplate> id,
        UuidId<Client> clientId,
        String code,
        String name,
        Channel channel,
        Language lang,
        String content,
        EntityStatus status
) {}