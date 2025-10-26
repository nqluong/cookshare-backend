package com.backend.cookshare.recipe_management.service;

import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RecipeService {

    RecipeResponse createRecipe(RecipeRequest request);

    RecipeResponse getRecipeById(UUID id);

    RecipeResponse updateRecipe(UUID id, RecipeRequest request);

    void deleteRecipe(UUID id);

    Page<RecipeResponse> getAllRecipes(Pageable pageable);

    List<RecipeResponse> getAllRecipesByUserId(UUID userId);
}
