package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.interaction.dto.response.RecipeSummaryResponse;
import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.request.RecipeStepRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.dto.response.RecipeStepResponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.entity.RecipeIngredient;
import com.backend.cookshare.recipe_management.entity.RecipeStep;
import com.backend.cookshare.user.dto.RecipeByFollowingResponse;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecipeMapper {
    RecipeMapper INSTANCE = Mappers.getMapper(RecipeMapper.class);

    // Mapping RecipeRequest → Recipe entity
    @Mapping(target = "recipeId", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    Recipe toEntity(RecipeRequest dto);

    // Recipe → Response
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "categories", ignore = true)
    RecipeResponse toResponse(Recipe entity);

    void updateRecipeFromDto(RecipeRequest dto, @MappingTarget Recipe entity);

    // ---- Mapping Steps & Ingredients ----
    RecipeStep toStepEntity(RecipeStepRequest dto);
    RecipeStepResponse toStepResponse(RecipeStep entity);

    RecipeIngredientResponse toIngredientResponse(RecipeIngredient entity);
    List<RecipeStepResponse> toStepResponseList(List<RecipeStep> entities);
    List<RecipeIngredientResponse> toIngredientResponseList(List<RecipeIngredient> entities);
    @Mappings({
            @Mapping(target = "followerId", ignore = true),
            @Mapping(target = "followingId", source = "recipe.user.userId"),
            @Mapping(target = "createdAt", source = "recipe.createdAt"),
            @Mapping(target = "recipe", source = "summary")
    })
    RecipeByFollowingResponse toRecipeByFollowingResponse(Recipe recipe, RecipeSummaryResponse summary);
    default RecipeSummaryResponse toRecipeSummary(Recipe recipe) {
        if (recipe == null) return null;

        return RecipeSummaryResponse.builder()
                .recipeId(recipe.getRecipeId())
                .title(recipe.getTitle())
                .slug(recipe.getSlug())
                .description(recipe.getDescription())
                .featuredImage(recipe.getFeaturedImage())
                .prepTime(recipe.getPrepTime())
                .cookTime(recipe.getCookTime())
                .servings(recipe.getServings())
                .difficulty(recipe.getDifficulty())
                .userId(recipe.getUser() != null ? recipe.getUser().getUserId() : null)
                .userName(recipe.getUser() != null ? recipe.getUser().getUsername() : null)
                .fullName(recipe.getUser() != null ? recipe.getUser().getFullName() : null)
                .viewCount(recipe.getViewCount())
                .saveCount(recipe.getSaveCount())
                .likeCount(recipe.getLikeCount())
                .averageRating(recipe.getAverageRating())
                .ratingCount(recipe.getRatingCount())
                .isPublished(recipe.getIsPublished())
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .build();
    }
}
