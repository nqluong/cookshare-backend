package com.backend.cookshare.recipe_management.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeIngredientResponse {
    UUID ingredientId;
    String name;
    String slug;
    String description;

    String quantity;
    String unit;
    String notes;
    Integer orderIndex;
}
