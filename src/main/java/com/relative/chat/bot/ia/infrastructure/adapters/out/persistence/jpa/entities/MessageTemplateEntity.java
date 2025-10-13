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
@Table(name = "message_template", schema = "chatbotia")
public class MessageTemplateEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity clientEntity;


    @Column(name = "name", nullable = false, length = 120)
    private String name;


    @ColumnDefault("'WHATSAPP'")
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;


    @ColumnDefault("'es'")
    @Column(name = "language", nullable = false, length = 10)
    private String language;


    @ColumnDefault("'UTILITY'")
    @Column(name = "category", nullable = false, length = 20)
    private String category;


    @ColumnDefault("'{}'::jsonb")
    @Column(name = "content", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> content;


    @Column(name = "provider_sid", length = 120)
    private String providerSid;


    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


}