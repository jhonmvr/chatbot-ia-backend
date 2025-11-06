package com.relative.chat.bot.ia.domain.common;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador basado en UUID para entidades de dominio.
 * 
 * @param <T> Tipo de la entidad
 */
public final class UuidId<T> implements Id<T> {
    
    private final UUID value;
    
    public UuidId(UUID value) {
        this.value = Objects.requireNonNull(value, "UUID no puede ser null");
    }
    
    /**
     * Crea un identificador a partir de un UUID existente.
     */
    public static <T> UuidId<T> of(UUID uuid) {
        return new UuidId<>(uuid);
    }
    
    /**
     * Genera un nuevo identificador UUID aleatorio.
     */
    public static <T> UuidId<T> newId() {
        return new UuidId<>(UUID.randomUUID());
    }
    
    /**
     * Obtiene el valor UUID del identificador.
     */
    public UUID value() {
        return value;
    }
    
    @Override
    public Object rawValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof UuidId<?> other && value.equals(other.value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}

