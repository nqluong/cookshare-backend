package com.backend.cookshare.interaction.entity.sevice;

import com.backend.cookshare.interaction.entity.dto.response.RecipeLikeResponse;
import java.util.UUID;


public interface RecipeLikeService {
    RecipeLikeResponse likerecipe(UUID recipeId);
    Boolean isRecipeLiked(UUID recipeId);
    void unlikerecipe(UUID recipeId);
}
