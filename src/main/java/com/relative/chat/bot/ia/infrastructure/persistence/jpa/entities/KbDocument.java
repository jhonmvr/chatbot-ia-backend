package com.relative.chat.bot.ia.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "kb_document", schema = "chatbotia")
public class KbDocument {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "kb_id", nullable = false)
    private Kb kb;

    @Column(name = "source_uri", length = Integer.MAX_VALUE)
    private String sourceUri;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "language", length = 10)
    private String language;

    @ColumnDefault("'READY'")
    @Column(name = "status", length = 20)
    private String status;

    @ColumnDefault("0")
    @Column(name = "chunk_count")
    private Integer chunkCount;

    @ColumnDefault("'{}'::jsonb")
    @Column(name = "metadata", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}