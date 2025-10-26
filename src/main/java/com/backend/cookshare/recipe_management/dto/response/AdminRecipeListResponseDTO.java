package com.backend.cookshare.recipe_management.dto.response;

import com.backend.cookshare.recipe_management.enums.Difficulty;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO cho danh sách công thức trong admin panel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminRecipeListResponseDTO {
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
    
    // Thông tin người dùng
    String username;
    String userFullName;
    String userEmail;
}
