package com.backend.cookshare.recipe_management.dto;

import com.backend.cookshare.recipe_management.enums.Difficulty;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeRequest {

    @NotNull(message = "userId không được để trống")
    UUID userId;

    @NotBlank(message = "title không được để trống")
    String title;

    String slug;

    @NotBlank(message = "description không được để trống")
    String description;

    @PositiveOrZero(message = "prepTime phải >= 0")
    Integer prepTime;

    @PositiveOrZero(message = "cookTime phải >= 0")
    Integer cookTime;

    @Positive(message = "servings phải > 0")
    Integer servings;

    Difficulty difficulty;

    String featuredImage;

    @NotBlank(message = "instructions không được để trống")
    String instructions;

    String notes;
    String nutritionInfo;

    Boolean isPublished;
    Boolean isFeatured;

    String metaKeywords;
    String seasonalTags;
}
