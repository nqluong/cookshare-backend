package com.backend.cookshare.interaction.mapper;

import com.backend.cookshare.interaction.entity.RecipeLike;
import com.backend.cookshare.interaction.dto.response.RecipeLikeResponse;
import com.backend.cookshare.interaction.dto.response.RecipeSummaryResponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface RecipeLikeMapper {

    RecipeLikeResponse toRecipeLikeResponse(RecipeLike recipeLike);

    @Mappings({
            @Mapping(target = "userId", source = "like.userId"),
            @Mapping(target = "recipeId", source = "like.recipeId"),
            @Mapping(target = "createdAt", source = "like.createdAt"),
            @Mapping(target = "recipe", source = "summary")
    })
    RecipeLikeResponse toRecipeResponse(RecipeLike like, RecipeSummaryResponse summary);

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
