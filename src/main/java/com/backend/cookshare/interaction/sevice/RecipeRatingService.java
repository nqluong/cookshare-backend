package com.backend.cookshare.interaction.sevice;

import com.backend.cookshare.interaction.dto.response.RecipeRatingResponse;

import java.util.UUID;

public interface RecipeRatingService {
    RecipeRatingResponse ratingrecipe(UUID recipeId, Integer rate);
    Boolean isRecipeRated(UUID recipeId);
    Integer getMyRating(UUID recipeId);
}
