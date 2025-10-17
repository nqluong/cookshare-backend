package com.backend.cookshare.recipe_management.entity;

import lombok.*;

import jakarta.persistence.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "recipe_ingredients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RecipeIngredientId.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeIngredient {

    @Id
    @Column(name = "recipe_id", columnDefinition = "uuid")
    UUID recipeId;

    @Id
    @Column(name = "ingredient_id", columnDefinition = "uuid")
    UUID ingredientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", insertable = false, updatable = false)
    Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", insertable = false, updatable = false)
    Ingredient ingredient;

    @Column(name = "quantity", length = 50)
    String quantity;

    @Column(name = "unit", length = 50)
    String unit;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

    @Column(name = "order_index")
    Integer orderIndex;
}
