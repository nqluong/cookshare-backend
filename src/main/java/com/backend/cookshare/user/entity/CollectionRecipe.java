package com.backend.cookshare.user.entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collection_recipes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CollectionRecipeId.class)
public class CollectionRecipe {

    @Id
    @Column(name = "collection_id", columnDefinition = "uuid")
    private UUID collectionId;

    @Id
    @Column(name = "recipe_id", columnDefinition = "uuid")
    private UUID recipeId;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
