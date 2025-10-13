package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad para referencias de vectores en diferentes backends
 * 
 * Nota: No usamos @MapsId ni relaciones JPA para evitar conflictos con Hibernate.
 * La FK chunk_id es manejada manualmente en el adapter usando queries nativos.
 */
@Getter
@Setter
@Entity
@Table(name = "kb_vector_ref", schema = "chatbotia")
public class KbVectorRefEntity {
    
    /**
     * ID del chunk (FK a kb_chunk, también es PK)
     */
    @Id
    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;
    
    /**
     * Backend de almacenamiento vectorial: 'pgvector', 'qdrant', 'pinecone', etc.
     */
    @ColumnDefault("'pgvector'")
    @Column(name = "backend", nullable = false, length = 40)
    private String backend;
    
    /**
     * Nombre del índice en el backend
     */
    @ColumnDefault("'kb_embedding_pgvector'")
    @Column(name = "index_name", nullable = false, length = 128)
    private String indexName;
    
    /**
     * ID del vector en el backend
     */
    @Column(name = "vector_id", nullable = false, length = 256)
    private String vectorId;
    
    /**
     * Fecha de creación
     */
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}