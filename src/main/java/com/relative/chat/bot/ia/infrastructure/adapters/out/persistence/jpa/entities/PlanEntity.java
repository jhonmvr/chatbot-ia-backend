package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;


import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.Id;

import jakarta.persistence.Table;

import lombok.Getter;

import lombok.Setter;

import org.hibernate.annotations.ColumnDefault;


import java.math.BigDecimal;

import java.time.OffsetDateTime;

import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "plan", schema = "chatbotia")
public class PlanEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;


    @Column(name = "code", nullable = false, length = 40)
    private String code;


    @Column(name = "name", nullable = false, length = 120)
    private String name;


    @ColumnDefault("0")
    @Column(name = "monthly_price_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPriceUsd;


    @ColumnDefault("'USD'")
    @Column(name = "currency", nullable = false, length = 10)
    private String currency;


    @Column(name = "msg_limit_month")
    private Long msgLimitMonth;


    @Column(name = "users_limit")
    private Integer usersLimit;


    @Column(name = "kb_tokens_limit_month")
    private Long kbTokensLimitMonth;


    @Column(name = "concurrent_sessions_limit")
    private Integer concurrentSessionsLimit;


    @ColumnDefault("365")
    @Column(name = "retention_days")
    private Integer retentionDays;


    @Column(name = "ai_model", length = 80)
    private String aiModel;


    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}