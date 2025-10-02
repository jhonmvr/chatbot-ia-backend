package com.relative.chat.bot.ia.domain.ports.messaging;

import com.relative.chat.bot.ia.domain.messaging.OutboundItem;
import com.relative.chat.bot.ia.domain.types.QueueStatus;

import java.time.Instant;
import java.util.List;

public interface OutboundQueueRepository {
  void save(OutboundItem item);
  List<OutboundItem> due(QueueStatus status, Instant now, int limit);
}
