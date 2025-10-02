package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.common.Id;
import com.relative.chat.bot.ia.domain.messaging.Message;
import java.util.List;

public interface MessageRepository {
  void save(Message message);
  List<Message> findByConversation(Id<?> conversationId, int limit);
}
