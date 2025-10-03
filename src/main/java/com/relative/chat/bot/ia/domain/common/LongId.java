package com.relative.chat.bot.ia.domain.common;

import java.util.Objects;

/**
 * Identificador basado en Long para entidades de dominio.
 * Usado principalmente para entidades con secuencias autogeneradas.
 * 
 * @param <T> Tipo de la entidad
 */
public final class LongId<T> implements Id<T> {
    
    private final Long value;
    
    private LongId(Long value) {
        this.value = value; // Puede ser null para entidades nuevas
    }
    
    /**
     * Crea un identificador a partir de un Long existente.
     */
    public static <T> LongId<T> of(Long value) {
        Objects.requireNonNull(value, "Long no puede ser null");
        return new LongId<>(value);
    }
    
    /**
     * Crea un identificador sin valor (para entidades nuevas).
     */
    public static <T> LongId<T> newId() {
        return new LongId<>(null);
    }
    
    /**
     * Obtiene el valor Long del identificador.
     * Puede ser null si es una entidad nueva.
     */
    public Long value() {
        return value;
    }
    
    @Override
    public Object rawValue() {
        return value;
    }
    
    /**
     * Verifica si el identificador tiene un valor asignado.
     */
    public boolean isPresent() {
        return value != null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof LongId<?> other) {
            return Objects.equals(value, other.value);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
    
    @Override
    public String toString() {
        return value != null ? value.toString() : "null";
    }
}

