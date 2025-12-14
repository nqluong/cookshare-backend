package com.backend.cookshare.recipe_management.dto.request;

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

    @Valid
    List<UUID> ingredients;

    @Valid
    @NotEmpty(message = "Phải có ít nhất 1 nguyên liệu với đầy đủ thông tin")
    List<IngredientDetailRequest> ingredientDetails;

    @Valid
    @NotEmpty(message = "Phải có ít nhất 1 bước nấu ăn")
    List<RecipeStepRequest> steps;

    List<UUID> tagIds;
    List<UUID> categoryIds;
    private List<CategoryRequest> newCategories;
    private List<TagRequest> newTags;
    private List<IngredientRequest> newIngredients;

    Boolean isPublished;
    String notes;
    String nutritionInfo;

    String metaKeywords;
    String seasonalTags;
    String instruction;
    String status;
}