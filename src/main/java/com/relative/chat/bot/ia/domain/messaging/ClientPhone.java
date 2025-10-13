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
        String providerSid,  // phone_number_id de Meta, SID de Twilio, etc.
        EntityStatus status,
        Instant verifiedAt
) {
    public Optional<Instant> verifiedAtOpt() {
        return Optional.ofNullable(verifiedAt);
    }
    
    public Optional<String> providerSidOpt() {
        return Optional.ofNullable(providerSid);
    }
}