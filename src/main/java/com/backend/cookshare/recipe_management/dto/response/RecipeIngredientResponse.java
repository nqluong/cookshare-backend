package com.backend.cookshare.recipe_management.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
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
    String category;

    String quantity;
    String unit;
    String notes;
    Integer orderIndex;
    LocalDateTime createdAt;
}
