package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ContactTagId implements Serializable {

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "tag_id", nullable = false)
    private UUID tagId;
}
