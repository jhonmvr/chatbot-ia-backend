package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "contact_tag", schema = "chatbotia")
public class ContactTagEntity {

    @EmbeddedId
    private ContactTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("contactId")
    @JoinColumn(name = "contact_id", nullable = false, foreignKey = @ForeignKey(name = "contact_tag_contact_id_fkey"))
    private ContactEntity contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false, foreignKey = @ForeignKey(name = "contact_tag_tag_id_fkey"))
    private TagEntity tag;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
