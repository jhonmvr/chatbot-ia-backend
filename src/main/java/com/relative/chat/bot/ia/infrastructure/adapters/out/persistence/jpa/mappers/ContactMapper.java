package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.domain.messaging.Contact;
import com.relative.chat.bot.ia.domain.types.EntityStatus;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ClientEntity;
import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.ContactEntity;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class ContactMapper {
    
    private ContactMapper() {}
    
    public static Contact toDomain(ContactEntity e) {
        if (e == null) return null;
        
        return new Contact(
                MappingHelpers.toUuidId(e.getId()),
                e.getClientEntity() != null ? MappingHelpers.toUuidId(e.getClientEntity().getId()) : null,
                e.getDisplayName(),
                e.getEmail() != null ? MappingHelpers.email(e.getEmail()) : null,
                e.getTags() != null ? String.join(",", e.getTags()) : null,
                e.getAttributes() != null && e.getAttributes().containsKey("status")
                        ? EntityStatus.valueOf((String) e.getAttributes().get("status"))
                        : EntityStatus.ACTIVE
        );
    }
    
    public static ContactEntity toEntity(Contact d) {
        if (d == null) return null;
        
        ContactEntity e = new ContactEntity();
        e.setId(d.id().value());
        e.setDisplayName(d.fullName());
        
        // Email - es un Email record, no Optional
        if (d.email() != null) {
            e.setEmail(MappingHelpers.email(d.email()));
        }
        
        e.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        e.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        
        // Tags
        if (d.tags() != null && !d.tags().isBlank()) {
            e.setTags(java.util.List.of(d.tags().split(",")));
        }
        
        // Attributes (requerido, nullable = false)
        java.util.Map<String, Object> attrs = new java.util.HashMap<>();
        if (d.status() != null) {
            attrs.put("status", d.status().name());
        }
        e.setAttributes(attrs); // Siempre setear, incluso si está vacío
        
        return e;
    }
    
    /**
     * Versión que necesita EntityManager para setear la relación con Client
     */
    public static ContactEntity toEntity(Contact d, EntityManager em) {
        ContactEntity e = toEntity(d);
        if (e != null && d.clientId() != null) {
            e.setClientEntity(em.getReference(ClientEntity.class, d.clientId().value()));
        }
        return e;
    }
}
