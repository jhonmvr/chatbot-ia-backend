package com.relative.chat.bot.ia.domain.common;

/**
 * Interfaz base para identificadores de dominio.
 * Permite usar UUID o Long según las necesidades de cada entidad.
 * 
 * @param <T> Tipo de la entidad
 */
public sealed interface Id<T> permits UuidId, LongId {
    
    /**
     * Obtiene el valor del identificador como Object.
     * Usar value() para obtener el tipo específico.
     */
    Object rawValue();
    
    /**
     * Crea un nuevo identificador UUID.
     */
    static <T> UuidId<T> newUuid() {
        return UuidId.newId();
    }
    
    /**
     * Crea un identificador UUID a partir de un UUID existente.
     */
    static <T> UuidId<T> ofUuid(java.util.UUID uuid) {
        return UuidId.of(uuid);
    }
    
    /**
     * Crea un identificador Long a partir de un Long existente.
     */
    static <T> LongId<T> ofLong(Long value) {
        return LongId.of(value);
    }
    
    /**
     * Crea un nuevo identificador Long (sin valor inicial).
     * Útil para entidades nuevas que aún no tienen ID.
     */
    static <T> LongId<T> newLong() {
        return LongId.newId();
    }
}
