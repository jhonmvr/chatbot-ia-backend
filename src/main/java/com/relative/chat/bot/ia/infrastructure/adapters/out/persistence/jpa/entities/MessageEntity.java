package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;


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
@Table(name = "message", schema = "chatbotia")
public class MessageEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity clientEntity;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversationEntity;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "contact_id", nullable = false)
    private ContactEntity contactEntity;


    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "phone_id")
    private ClientPhoneEntity phone;


    @Column(name = "direction", nullable = false, length = 20)
    private String direction;


    @Column(name = "channel", nullable = false, length = 20)
    private String channel;


    @Column(name = "provider", length = 20)
    private String provider;


    @ColumnDefault("'TEXT'")
    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;


    @Column(name = "body", length = Integer.MAX_VALUE)
    private String body;


    @ColumnDefault("'{}'::jsonb")
    @Column(name = "media", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> media;


    @Column(name = "status", nullable = false, length = 20)
    private String status;


    @Column(name = "error_code", length = 50)
    private String errorCode;


    @Column(name = "raw_payload")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rawPayload;


    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


    @Column(name = "sent_at")
    private OffsetDateTime sentAt;


    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;


    @Column(name = "read_at")
    private OffsetDateTime readAt;


}