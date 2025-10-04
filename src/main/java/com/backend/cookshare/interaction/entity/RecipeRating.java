package com.backend.cookshare.interaction.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recipe_ratings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRating {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "rating_id", columnDefinition = "uuid")
    private UUID ratingId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "recipe_id", nullable = false, columnDefinition = "uuid")
    private UUID recipeId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "review", columnDefinition = "TEXT")
    private String review;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
