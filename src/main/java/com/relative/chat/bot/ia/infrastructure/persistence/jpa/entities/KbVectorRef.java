package com.relative.chat.bot.ia.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "kb_vector_ref", schema = "chatbotia")
public class KbVectorRef {
    @Id
    @Column(name = "chunk_id", nullable = false)
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "chunk_id", nullable = false)
    private KbChunk kbChunk;

    @ColumnDefault("'pgvector'")
    @Column(name = "backend", nullable = false, length = 40)
    private String backend;

    @ColumnDefault("'kb_embedding_pgvector'")
    @Column(name = "index_name", nullable = false, length = 128)
    private String indexName;

    @Column(name = "vector_id", nullable = false, length = 256)
    private String vectorId;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

}