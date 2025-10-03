package com.relative.chat.bot.ia.domain.ports.identity;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Subscription;
import java.util.Optional;

public interface SubscriptionRepository {
    Optional<Subscription> findById(UuidId<Subscription> id);
    void save(Subscription subscription);
}
