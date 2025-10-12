package com.backend.cookshare.recipe_management.service;

import com.backend.cookshare.recipe_management.dto.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.RecipeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Interface định nghĩa các chức năng CRUD cho Recipe
 */
public interface RecipeService {
    RecipeResponse createRecipe(RecipeRequest request);
    RecipeResponse getRecipeById(UUID id);
    RecipeResponse updateRecipe(UUID id, RecipeRequest request);
    void deleteRecipe(UUID id);
    Page<RecipeResponse> getAllRecipes(Pageable pageable);
}
