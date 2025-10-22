package com.relative.chat.bot.ia.domain.messaging;

import com.relative.chat.bot.ia.domain.common.UuidId;

import java.time.Instant;
import java.util.List;

/**
 * Categoría para clasificar contactos
 */
public record Category(
        UuidId<Category> id,
        String name,
        String description,
        String color,
        String icon,
        Boolean isActive,
        Integer sortOrder,
        List<UuidId<Contact>> contactIds,
        Instant createdAt,
        Instant updatedAt
) {
    
    /**
     * Constructor para nueva categoría
     */
    public static Category create(
            String name,
            String description,
            String color,
            String icon,
            Integer sortOrder
    ) {
        Instant now = Instant.now();
        return new Category(
                UuidId.newId(),
                name,
                description,
                color,
                icon,
                true, // isActive por defecto
                sortOrder != null ? sortOrder : 0,
                List.of(), // contactIds vacío inicialmente
                now,
                now
        );
    }
    
    /**
     * Constructor para categoría existente
     */
    public static Category existing(
            UuidId<Category> id,
            String name,
            String description,
            String color,
            String icon,
            Boolean isActive,
            Integer sortOrder,
            List<UuidId<Contact>> contactIds,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Category(
                id, name, description, color, icon, isActive, sortOrder,
                contactIds, createdAt, updatedAt
        );
    }
    
    /**
     * Verifica si la categoría está activa
     */
    public Boolean isActive() {
        return isActive != null && isActive;
    }
    
    /**
     * Actualiza el nombre
     */
    public Category withName(String newName) {
        return new Category(id, newName, description, color, icon, isActive, sortOrder, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza la descripción
     */
    public Category withDescription(String newDescription) {
        return new Category(id, name, newDescription, color, icon, isActive, sortOrder, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza el color
     */
    public Category withColor(String newColor) {
        return new Category(id, name, description, newColor, icon, isActive, sortOrder, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza el icono
     */
    public Category withIcon(String newIcon) {
        return new Category(id, name, description, color, newIcon, isActive, sortOrder, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza el estado activo
     */
    public Category withIsActive(Boolean newIsActive) {
        return new Category(id, name, description, color, icon, newIsActive, sortOrder, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza el orden de clasificación
     */
    public Category withSortOrder(Integer newSortOrder) {
        return new Category(id, name, description, color, icon, isActive, newSortOrder, contactIds, createdAt, Instant.now());
    }
    
    /**
     * Actualiza la lista de contactos
     */
    public Category withContactIds(List<UuidId<Contact>> newContactIds) {
        return new Category(id, name, description, color, icon, isActive, sortOrder, newContactIds, createdAt, Instant.now());
    }
}
