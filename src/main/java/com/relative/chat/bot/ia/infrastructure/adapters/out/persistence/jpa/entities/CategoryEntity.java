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
@Table(name = "category", schema = "chatbotia")
public class CategoryEntity {
    
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "color", length = 7) // Hex color code
    private String color;
    
    @Column(name = "icon", length = 50) // Icon name or class
    private String icon;
    
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private Boolean isActive = true;
    
    @Column(name = "sort_order", nullable = false)
    @ColumnDefault("0")
    private Integer sortOrder = 0;
    
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    // Relación many-to-many con ContactEntity
    @ManyToMany(mappedBy = "categories", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ContactEntity> contacts = new ArrayList<>();
}
