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
@Table(name = "client_phone", schema = "chatbotia")
public class ClientPhoneEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity clientEntity;


    @ColumnDefault("'WHATSAPP'")
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;


    @Column(name = "e164", nullable = false, length = 20)
    private String e164;


    @ColumnDefault("'TWILIO'")
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;


    @Column(name = "provider_sid", length = 120)
    private String providerSid;


    @Column(name = "webhook_secret", length = 120)
    private String webhookSecret;


    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;


    @ColumnDefault("false")
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;


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