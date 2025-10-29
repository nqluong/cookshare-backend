package com.backend.cookshare.interaction.sevice;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.interaction.dto.response.RecipeLikeResponse;
import java.util.UUID;


public interface RecipeLikeService {
    RecipeLikeResponse likerecipe(UUID recipeId);
    Boolean isRecipeLiked(UUID recipeId);
    void unlikerecipe(UUID recipeId);
    PageResponse<RecipeLikeResponse> getallRecipeLiked(int page, int size);
}
