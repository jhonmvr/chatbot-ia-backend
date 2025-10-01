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
@Table(name = "conversation", schema = "chatbotia")
public class Conversation {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "phone_id")
    private ClientPhone phone;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "title", length = 250)
    private String title;

    @ColumnDefault("'OPEN'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @ColumnDefault("now()")
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}