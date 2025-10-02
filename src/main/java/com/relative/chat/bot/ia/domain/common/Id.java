package com.relative.chat.bot.ia.domain.common;

import java.util.*;

public final class Id<T>{
    private final UUID v;

    private Id(UUID v){
        this.v=Objects.requireNonNull(v);

    }
    public static <T> Id<T> of(UUID u){
        return new Id<>(u);

    }
    public static <T> Id<T> newId(){
        return new Id<>(UUID.randomUUID());

    }
    public UUID value(){
        return v;

    }
    @Override
    public boolean equals(Object o){
        return o instanceof Id<?> i && v.equals(i.v);

    }
    @Override public int hashCode(){
        return v.hashCode();

    }
}
