package com.backend.cookshare.recipe_management.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingredients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ingredient_id", columnDefinition = "uuid")
    private UUID ingredientId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", unique = true, length = 255)
    private String slug;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
