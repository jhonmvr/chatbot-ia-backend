package com.relative.chat.bot.ia.infrastructure.adapters.out.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tag", schema = "chatbotia")
public class TagEntity {
    
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "color", length = 7) // Hex color code
    private String color;
    
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private Boolean isActive = true;
    
    @Column(name = "usage_count", nullable = false)
    @ColumnDefault("0")
    private Integer usageCount = 0;
    
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    // Relación many-to-many con ContactEntity (LAZY para evitar carga innecesaria)
    @ManyToMany(mappedBy = "tags", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<ContactEntity> contacts = new ArrayList<>();
}
