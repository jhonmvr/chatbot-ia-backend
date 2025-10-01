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
@Table(name = "outbound_queue", schema = "chatbotia")
public class OutboundQueue {
    @Id
    @ColumnDefault("nextval('chatbotia.outbound_queue_id_seq'::regclass)")
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "phone_id")
    private ClientPhone phone;

    @ColumnDefault("'WHATSAPP'")
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "body", length = Integer.MAX_VALUE)
    private String body;

    @ColumnDefault("'{}'::jsonb")
    @Column(name = "media", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> media;

    @Column(name = "schedule_at")
    private OffsetDateTime scheduleAt;

    @ColumnDefault("'QUEUED'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @ColumnDefault("0")
    @Column(name = "retries", nullable = false)
    private Integer retries;

    @Column(name = "last_error", length = Integer.MAX_VALUE)
    private String lastError;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}