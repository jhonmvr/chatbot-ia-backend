package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.messaging.Conversation;
import com.relative.chat.bot.ia.domain.messaging.Message;
import java.util.List;

public interface MessageRepository {
    void save(Message message);
    List<Message> findByConversation(UuidId<Conversation> conversationId, int limit);
}
