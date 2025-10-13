package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;


import jakarta.persistence.*;

import lombok.Getter;

import lombok.Setter;

import org.hibernate.annotations.ColumnDefault;

import org.hibernate.annotations.OnDelete;

import org.hibernate.annotations.OnDeleteAction;


import java.math.BigDecimal;

import java.time.LocalDate;

import java.time.OffsetDateTime;


@Getter
@Setter
@Entity
@Table(name = "usage_daily", schema = "chatbotia")
public class UsageDailyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usage_daily_id_gen")
    @SequenceGenerator(name = "usage_daily_id_gen", sequenceName = "usage_daily_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity clientEntity;


    @Column(name = "day", nullable = false)
    private LocalDate day;


    @ColumnDefault("0")
    @Column(name = "messages_in", nullable = false)
    private Long messagesIn;


    @ColumnDefault("0")
    @Column(name = "messages_out", nullable = false)
    private Long messagesOut;


    @ColumnDefault("0")
    @Column(name = "tokens_in", nullable = false)
    private Long tokensIn;


    @ColumnDefault("0")
    @Column(name = "tokens_out", nullable = false)
    private Long tokensOut;


    @ColumnDefault("0")
    @Column(name = "vector_mb", nullable = false, precision = 12, scale = 2)
    private BigDecimal vectorMb;


    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}