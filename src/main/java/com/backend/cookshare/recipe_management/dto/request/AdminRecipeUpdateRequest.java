package com.backend.cookshare.recipe_management.dto.request;

import com.backend.cookshare.recipe_management.enums.Difficulty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminRecipeUpdateRequest {
    String title;
    String description;
    Integer prepTime;
    Integer cookTime;
    Integer servings;
    Difficulty difficulty;
    String featuredImage;
    String instructions;
    String notes;
    String nutritionInfo;
    Boolean isPublished;
    Boolean isFeatured;
    String metaKeywords;
    String seasonalTags;
    BigDecimal averageRating;
    Integer ratingCount;
}
