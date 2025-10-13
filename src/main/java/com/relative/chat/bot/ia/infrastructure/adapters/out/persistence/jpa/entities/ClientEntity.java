package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;


import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.Id;

import jakarta.persistence.Table;

import lombok.Getter;

import lombok.Setter;

import org.hibernate.annotations.ColumnDefault;

import org.hibernate.annotations.JdbcTypeCode;

import org.hibernate.type.SqlTypes;


import java.time.OffsetDateTime;

import java.util.Map;

import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "client", schema = "chatbotia")
public class ClientEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;


    @Column(name = "name", nullable = false, length = 200)
    private String name;


    @Column(name = "tax_id", length = 50)
    private String taxId;


    @Column(name = "domain", length = 200)
    private String domain;


    @ColumnDefault("'America/Guayaquil'")
    @Column(name = "timezone", length = 50)
    private String timezone;


    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", length = 30)
    private String status;


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