package com.backend.cookshare.interaction.mapper;

import com.backend.cookshare.interaction.dto.response.RecipeRatingResponse;
import com.backend.cookshare.interaction.entity.RecipeRating;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecipeRatingMapper {
    RecipeRatingResponse toRecipeRatingResponse(RecipeRating rating);
}
