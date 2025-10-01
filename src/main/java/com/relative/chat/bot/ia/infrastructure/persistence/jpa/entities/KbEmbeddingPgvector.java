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
@Table(name = "kb_embedding_pgvector", schema = "chatbotia")
public class KbEmbeddingPgvector {
    @Id
    @Column(name = "chunk_id", nullable = false)
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "chunk_id", nullable = false)
    private KbChunk kbChunk;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "kb_id")
    private UUID kbId;
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

/*
 TODO [Reverse Engineering] create field to map the 'embedding' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "embedding", columnDefinition = "vector(1536) not null")
    private Object embedding;
*/
}