package com.relative.chat.bot.ia.domain.knowledge;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.identity.*;
import java.util.*;

public final class Kb {
    
    private final UuidId<Kb> id;
    private final UuidId<Client> clientId;
    private String name;
    private String description;
    
    public Kb(UuidId<Kb> id, UuidId<Client> clientId, String name, String description) {
        this.id = Objects.requireNonNull(id);
        this.clientId = Objects.requireNonNull(clientId);
        if (name == null || name.isBlank()) {
            throw new DomainException("KB.name requerido");
        }
        this.name = name;
        this.description = description;
    }
    
    public UuidId<Kb> id() {
        return id;
    }
    
    public UuidId<Client> clientId() {
        return clientId;
    }
    
    public String name() {
        return name;
    }
    
    public String description() {
        return description;
    }
}