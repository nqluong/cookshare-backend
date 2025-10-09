package com.backend.cookshare.recipe_management.dto;

import com.backend.cookshare.recipe_management.enums.Difficulty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO nhận dữ liệu khi tạo hoặc cập nhật Recipe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeRequest {

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
}
