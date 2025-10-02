package com.relative.chat.bot.ia.domain.ports.identity;

import com.relative.chat.bot.ia.domain.common.Id;
import com.relative.chat.bot.ia.domain.identity.Subscription;
import java.util.Optional;

public interface SubscriptionRepository {
  Optional<Subscription> findById(Id<Subscription> id);
  void save(Subscription subscription);
}
