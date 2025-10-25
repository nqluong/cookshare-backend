package com.backend.cookshare.recipe_management.service;

import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;

import java.util.List;
import java.util.UUID;

public interface IngredientService {
    RecipeIngredientResponse createIngredient(IngredientRequest request);
    RecipeIngredientResponse getIngredientById(UUID id);
    RecipeIngredientResponse updateIngredient(UUID id, IngredientRequest request);
    void deleteIngredient(UUID id);
    List<RecipeIngredientResponse> getAllIngredients();
}
