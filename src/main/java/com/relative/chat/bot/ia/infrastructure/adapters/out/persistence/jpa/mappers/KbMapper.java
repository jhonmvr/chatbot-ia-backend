package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.mappers;

import com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities.*;
import com.relative.chat.bot.ia.domain.knowledge.*;

public final class KbMapper {
    
    private KbMapper() {}
    
    public static Kb toDomain(KbEntity e) {
        if (e == null) return null;
        
        return new Kb(
            MappingHelpers.toUuidId(e.getId()),
            e.getClientEntity() != null ? MappingHelpers.toUuidId(e.getClientEntity().getId()) : null,
            e.getName(),
            e.getDescription()
        );
    }
    
    public static KbEntity toEntity(Kb d, ClientEntity clientEntity) {
        if (d == null) return null;
        
        KbEntity e = new KbEntity();
        e.setId(MappingHelpers.toUuid(d.id()));
        e.setClientEntity(clientEntity);
        e.setName(d.name());
        e.setDescription(d.description());
        e.setCreatedAt(java.time.OffsetDateTime.now());
        e.setUpdatedAt(java.time.OffsetDateTime.now());
        
        return e;
    }
    
    public static KbDocument toDomain(KbDocumentEntity e) {
        if (e == null) return null;
        
        return new KbDocument(
            MappingHelpers.toUuidId(e.getId()),
            e.getKbEntity() != null ? MappingHelpers.toUuidId(e.getKbEntity().getId()) : null,
            e.getSourceUri(),
            null,
            null,
            e.getMimeType(),
            null,
            null,
            e.getLanguage()
        );
    }
    
    public static KbChunk toDomain(KbChunkEntity e) {
        if (e == null) return null;
        
        return new KbChunk(
            MappingHelpers.toUuidId(e.getId()),
            e.getDocument() != null ? MappingHelpers.toUuidId(e.getDocument().getId()) : null,
            e.getChunkIndex(),
            e.getContent(),
            null
        );
    }
    
    public static VectorRef toDomain(KbVectorRefEntity e) {
        if (e == null) return null;
        
        return new VectorRef(
            MappingHelpers.toUuidId(e.getChunkId()),
            com.relative.chat.bot.ia.domain.types.VectorBackend.valueOf(e.getBackend()),
            e.getIndexName(),
            e.getVectorId()
        );
    }
}
