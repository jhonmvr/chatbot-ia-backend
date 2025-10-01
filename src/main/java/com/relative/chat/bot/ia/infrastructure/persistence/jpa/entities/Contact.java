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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "contact", schema = "chatbotia")
public class Contact {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "external_id", length = 120)
    private String externalId;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "phone_e164", length = 20)
    private String phoneE164;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "locale", length = 10)
    private String locale;

    @Column(name = "tags")
    private List<String> tags;

    @ColumnDefault("'{}'::jsonb")
    @Column(name = "attributes", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> attributes;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}