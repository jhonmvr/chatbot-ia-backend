package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;

import java.time.Instant;
import java.util.List;

/**
 * Etiqueta para etiquetar contactos
 */
public record Tag(
        UuidId<Tag> id,
        String name,
        String description,
        String color,
        Boolean isActive,
        Integer usageCount,
        List<UuidId<Contact>> contactIds,
        Instant createdAt,
        Instant updatedAt
) {
    
    /**
     * Constructor para nueva etiqueta
     */
    public static Tag create(
            String name,
            String description,
            String color
    ) {
        Instant now = Instant.now();
        return new Tag(
                UuidId.newId(),
                name,
                description,
                color,
                true, // isActive por defecto
                0, // usageCount inicial
                List.of(), // contactIds vacío inicialmente
                now,
                now
        );
    }
    
    /**
     * Constructor para etiqueta existente
     */
    public static Tag existing(
            UuidId<Tag> id,
            String name,
            String description,
            String color,
            Boolean isActive,
            Integer usageCount,
            List<UuidId<Contact>> contactIds,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Tag(
                id, name, description, color, isActive, usageCount,
                contactIds, createdAt, updatedAt
        );
    }
    
    /**
     * Verifica si la etiqueta está activa
     */
    public Boolean isActive() {
        return isActive != null && isActive;
    }
    
    /**
     * Actualiza el nombre
     */
    public Tag withName(String newName) {
        return new Tag(id, newName, description, color, isActive, usageCount, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza la descripción
     */
    public Tag withDescription(String newDescription) {
        return new Tag(id, name, newDescription, color, isActive, usageCount, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza el color
     */
    public Tag withColor(String newColor) {
        return new Tag(id, name, description, newColor, isActive, usageCount, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza el estado activo
     */
    public Tag withIsActive(Boolean newIsActive) {
        return new Tag(id, name, description, color, newIsActive, usageCount, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza el contador de uso
     */
    public Tag withUsageCount(Integer newUsageCount) {
        return new Tag(id, name, description, color, isActive, newUsageCount, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Incrementa el contador de uso
     */
    public Tag incrementUsageCount() {
        return new Tag(id, name, description, color, isActive, usageCount + 1, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Decrementa el contador de uso
     */
    public Tag decrementUsageCount() {
        int newCount = Math.max(0, usageCount - 1);
        return new Tag(id, name, description, color, isActive, newCount, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza la lista de contactos
     */
    public Tag withContactIds(List<UuidId<Contact>> newContactIds) {
        return new Tag(id, name, description, color, isActive, usageCount, newContactIds, createdAt, Instant.now());
    }
}
