package com.backend.cookshare.recipe_management.service;

import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface RecipeService {

    RecipeResponse createRecipe(RecipeRequest request);

    RecipeResponse createRecipeWithFiles(RecipeRequest request, MultipartFile image, List<MultipartFile> stepImages);

    RecipeResponse updateRecipe(UUID id, RecipeRequest request, MultipartFile image, List<MultipartFile> stepImages);

    RecipeResponse getRecipeById(UUID id);

    void deleteRecipe(UUID id);

    Page<RecipeResponse> getAllRecipes(Pageable pageable);

    List<RecipeResponse> getAllRecipesByUserId(UUID userId, UUID currentUserId);

    RecipeResponse togglePrivacy(UUID id);
}
