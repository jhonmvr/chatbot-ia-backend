package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;


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
@Table(name = "kb", schema = "chatbotia")
public class KbEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity clientEntity;


    @Column(name = "name", nullable = false, length = 200)
    private String name;


    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;


    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


}