package com.backend.cookshare.recipe_management.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "recipe_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RecipeCategoryId.class)
public class RecipeCategory {

    @Id
    @Column(name = "recipe_id", columnDefinition = "uuid")
    private UUID recipeId;

    @Id
    @Column(name = "category_id", columnDefinition = "uuid")
    private UUID categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", insertable = false, updatable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;
}
