package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.identity.*;
import com.relative.chat.bot.ia.domain.types.*;
import com.relative.chat.bot.ia.domain.vo.*;
import java.time.*;
import java.util.*;

public record ClientPhone(
        UuidId<ClientPhone> id,
        UuidId<Client> clientId,
        PhoneE164 phone,
        Channel channel,
        String provider,
        EntityStatus status,
        Instant verifiedAt
) {
    public Optional<Instant> verifiedAtOpt() {
        return Optional.ofNullable(verifiedAt);
    }
}