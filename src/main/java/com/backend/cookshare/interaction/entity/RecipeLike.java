package com.backend.cookshare.interaction.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recipe_likes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RecipeLikeId.class)
public class RecipeLike {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Id
    @Column(name = "recipe_id", columnDefinition = "uuid")
    private UUID recipeId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
