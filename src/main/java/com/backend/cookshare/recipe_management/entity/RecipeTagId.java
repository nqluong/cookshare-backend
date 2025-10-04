package com.backend.cookshare.recipe_management.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeTagId implements Serializable {
    private UUID recipeId;
    private UUID tagId;
}
