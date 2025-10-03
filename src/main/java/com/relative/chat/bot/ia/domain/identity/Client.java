package com.relative.chat.bot.ia.domain.identity;

import com.relative.chat.bot.ia.domain.common.*;
import com.relative.chat.bot.ia.domain.types.*;
import java.util.*;

public final class Client {
    
    private final UuidId<Client> id;
    private String code;
    private String name;
    private EntityStatus status;
    
    public Client(UuidId<Client> id, String code, String name, EntityStatus status) {
        this.id = Objects.requireNonNull(id);
        this.code = require(code);
        this.name = require(name);
        this.status = status == null ? EntityStatus.ACTIVE : status;
    }
    
    private static String require(String s) {
        if (s == null || s.isBlank()) {
            throw new DomainException("Campo requerido");
        }
        return s;
    }
    
    public UuidId<Client> id() {
        return id;
    }
    
    public String code() {
        return code;
    }
    
    public String name() {
        return name;
    }
    
    public EntityStatus status() {
        return status;
    }
}