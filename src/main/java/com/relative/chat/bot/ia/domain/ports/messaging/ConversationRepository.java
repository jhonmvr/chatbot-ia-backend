package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import java.util.Optional;

public interface ConversationRepository {
    Optional<Conversation> findById(UuidId<Conversation> id);
    void save(Conversation conversation);
}
