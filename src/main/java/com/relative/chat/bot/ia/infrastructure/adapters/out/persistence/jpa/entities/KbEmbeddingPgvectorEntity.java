package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad para almacenar embeddings vectoriales en pgvector
 * 
 * Nota: No usamos @MapsId ni relaciones JPA para evitar conflictos con Hibernate.
 * La FK chunk_id es manejada manualmente en el adapter usando queries nativos.
 */
@Getter
@Setter
@Entity
@Table(name = "kb_embedding_pgvector", schema = "chatbotia")
public class KbEmbeddingPgvectorEntity {
    
    /**
     * ID del chunk (FK a kb_chunk, también es PK)
     */
    @Id
    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;
    
    /**
     * Vector embedding en formato pgvector string: "[0.1, 0.2, 0.3]"
     */
    @Column(name = "embedding", columnDefinition = "vector not null")
    private String embedding;
    
    /**
     * ID del cliente (para filtros)
     */
    @Column(name = "client_id")
    private UUID clientId;
    
    /**
     * ID del Knowledge Base (para filtros)
     */
    @Column(name = "kb_id")
    private UUID kbId;
    
    /**
     * Fecha de creación
     */
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}