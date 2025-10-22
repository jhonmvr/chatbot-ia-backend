package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.messaging.Tag;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.TagEntity;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

public final class TagMapper {
    
    private TagMapper() {}
    
    public static Tag toDomain(TagEntity e) {
        if (e == null) return null;
        
        return Tag.existing(
                MappingHelpers.toUuidId(e.getId()),
                e.getName(),
                e.getDescription(),
                e.getColor(),
                e.getIsActive(),
                e.getUsageCount(),
                Collections.emptyList(), // contactIds se carga por separado
                e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null,
                e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null
        );
    }
    
    public static TagEntity toEntity(Tag d) {
        if (d == null) return null;
        
        TagEntity e = new TagEntity();
        e.setId(d.id().value());
        e.setName(d.name());
        e.setDescription(d.description());
        e.setColor(d.color());
        e.setIsActive(d.isActive());
        e.setUsageCount(d.usageCount());
        e.setCreatedAt(d.createdAt() != null ? OffsetDateTime.ofInstant(d.createdAt(), ZoneOffset.UTC) : OffsetDateTime.now());
        e.setUpdatedAt(d.updatedAt() != null ? OffsetDateTime.ofInstant(d.updatedAt(), ZoneOffset.UTC) : OffsetDateTime.now());
        
        return e;
    }
    
    /**
     * Versión que necesita EntityManager para manejar relaciones
     */
    public static TagEntity toEntity(Tag d, EntityManager em) {
        TagEntity e = toEntity(d);
        // No hay clientEntity que establecer
        return e;
    }
}
