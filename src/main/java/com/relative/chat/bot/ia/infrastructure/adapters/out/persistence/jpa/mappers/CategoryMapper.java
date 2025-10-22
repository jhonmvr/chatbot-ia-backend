package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.messaging.Category;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.CategoryEntity;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

public final class CategoryMapper {
    
    private CategoryMapper() {}
    
    public static Category toDomain(CategoryEntity e) {
        if (e == null) return null;
        
        return Category.existing(
                MappingHelpers.toUuidId(e.getId()),
                e.getName(),
                e.getDescription(),
                e.getColor(),
                e.getIcon(),
                e.getIsActive(),
                e.getSortOrder(),
                Collections.emptyList(), // contactIds se carga por separado
                e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null,
                e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null
        );
    }
    
    public static CategoryEntity toEntity(Category d) {
        if (d == null) return null;
        
        CategoryEntity e = new CategoryEntity();
        e.setId(d.id().value());
        e.setName(d.name());
        e.setDescription(d.description());
        e.setColor(d.color());
        e.setIcon(d.icon());
        e.setIsActive(d.isActive());
        e.setSortOrder(d.sortOrder());
        e.setCreatedAt(d.createdAt() != null ? OffsetDateTime.ofInstant(d.createdAt(), ZoneOffset.UTC) : OffsetDateTime.now());
        e.setUpdatedAt(d.updatedAt() != null ? OffsetDateTime.ofInstant(d.updatedAt(), ZoneOffset.UTC) : OffsetDateTime.now());
        
        return e;
    }
    
    /**
     * Versión que necesita EntityManager para manejar relaciones
     */
    public static CategoryEntity toEntity(Category d, EntityManager em) {
        CategoryEntity e = toEntity(d);
        // No hay clientEntity que establecer
        return e;
    }
}
