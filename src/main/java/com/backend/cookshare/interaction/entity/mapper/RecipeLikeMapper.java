package com.backend.cookshare.interaction.entity.mapper;

import com.backend.cookshare.interaction.entity.RecipeLike;
import com.backend.cookshare.interaction.entity.dto.response.RecipeLikeResponse;
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
            @Mapping(target = "recipe", source = "recipe")
    })
    RecipeLikeResponse toRecipeResponse(RecipeLike like, Recipe recipe);
}
