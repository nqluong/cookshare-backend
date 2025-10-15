package com.backend.cookshare.recipe_management.dto;

import com.backend.cookshare.recipe_management.enums.Difficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
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

    // 🔥 Thêm danh sách nguyên liệu
    @Valid
    @NotEmpty(message = "Phải có ít nhất 1 nguyên liệu")
    List<RecipeIngredientRequest> ingredients;

    // 🔥 Thêm danh sách bước nấu
    @Valid
    @NotEmpty(message = "Phải có ít nhất 1 bước nấu ăn")
    List<RecipeStepRequest> steps;

    // 🔥 Thêm tag + category
    List<UUID> tagIds;
    List<UUID> categoryIds;

    Boolean isPublished;
    Boolean isFeatured;

    String notes;
    String nutritionInfo;

    String metaKeywords;
    String seasonalTags;
}
