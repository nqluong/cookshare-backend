package com.backend.cookshare.recipe_management.dto;

import com.backend.cookshare.recipe_management.enums.Difficulty;
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

    // 🔥 Thêm chi tiết liên quan
    List<RecipeStepResponse> steps;
    List<RecipeIngredientResponse> ingredients;
    List<TagResponse> tags;
    List<CategoryResponse> categories;

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

    // 🔸 Các class con gọn gàng
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeStepResponse {
        Integer stepNumber;
        String instruction;
        String imageUrl;
        String videoUrl;
        Integer estimatedTime;
        String tips;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeIngredientResponse {
        UUID ingredientId;
        String name;
        String quantity;
        String unit;
        String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagResponse {
        UUID tagId;
        String name;
        String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        UUID categoryId;
        String name;
        String iconUrl;
    }
}
