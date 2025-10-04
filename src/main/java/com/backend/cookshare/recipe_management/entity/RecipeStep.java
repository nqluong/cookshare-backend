package com.backend.cookshare.recipe_management.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recipe_steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "step_id", columnDefinition = "uuid")
    UUID stepId;

    @Column(name = "recipe_id", nullable = false, columnDefinition = "uuid")
    UUID recipeId;

    @Column(name = "step_number", nullable = false)
    Integer stepNumber;

    @Column(name = "instruction", nullable = false, columnDefinition = "TEXT")
    String instruction;

    @Column(name = "image_url", length = 255)
    String imageUrl;

    @Column(name = "video_url", length = 255)
    String videoUrl;

    @Column(name = "estimated_time")
    Integer estimatedTime;

    @Column(name = "tips", columnDefinition = "TEXT")
    String tips;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
