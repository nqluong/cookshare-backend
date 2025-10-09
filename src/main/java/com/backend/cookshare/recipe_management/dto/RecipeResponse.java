package com.backend.cookshare.recipe_management.dto;

import com.backend.cookshare.recipe_management.enums.Difficulty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO trả về khi client gọi API Recipe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeResponse {

    UUID recipeId;
    UUID userId;
    String title;
    String slug;
    String description;
    Integer prepTime;
    Integer cookTime;
    Integer servings;
    Difficulty difficulty;
    String featuredImage;
    String instructions;
    String notes;
    String nutritionInfo;
    Integer viewCount;
    Integer saveCount;
    Integer likeCount;
    BigDecimal averageRating;
    Integer ratingCount;
    Boolean isPublished;
    Boolean isFeatured;
    String metaKeywords;
    String seasonalTags;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
