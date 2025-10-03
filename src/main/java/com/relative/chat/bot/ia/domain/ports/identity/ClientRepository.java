package com.relative.chat.bot.ia.domain.ports.identity;

import com.relative.chat.bot.ia.domain.common.UuidId;
import com.relative.chat.bot.ia.domain.identity.Client;
import java.util.Optional;

public interface ClientRepository {
    Optional<Client> findById(UuidId<Client> id);
    Optional<Client> findByCode(String code);
    void save(Client client);
}
