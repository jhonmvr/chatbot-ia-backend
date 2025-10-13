package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.identity.*;
import com.relative.chat.bot.ia.domain.types.*;
import com.relative.chat.bot.ia.domain.vo.*;
import java.util.*;

public record Contact(
        UuidId<Contact> id,
        UuidId<Client> clientId,
        String fullName,
        Email email,
        String tags,
        EntityStatus status
) {
    public Optional<Email> emailOpt() {
        return Optional.ofNullable(email);
    }
}