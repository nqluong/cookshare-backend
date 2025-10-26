package com.backend.cookshare.recipe_management.dto.response;

import com.backend.cookshare.recipe_management.dto.CategoryResponse;
import com.backend.cookshare.recipe_management.dto.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.dto.RecipeStepResponse;
import com.backend.cookshare.recipe_management.dto.TagResponse;
import com.backend.cookshare.recipe_management.enums.Difficulty;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminRecipeDetailResponseDTO {
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
    RecipeStatus status;
    String metaKeywords;
    String seasonalTags;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    String username;
    String userFullName;
    String userEmail;
    String userAvatarUrl;

    List<RecipeIngredientResponse> ingredients;

    List<RecipeStepResponse> steps;

    List<CategoryResponse> categories;

    List<TagResponse> tags;
}
