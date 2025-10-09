package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.recipe_management.dto.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.RecipeResponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.mapper.RecipeMapper;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.recipe_management.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    @Override
    public RecipeResponse createRecipe(RecipeRequest request) {
        Recipe recipe = recipeMapper.toEntity(request);

        // Generate slug nếu null
        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        Recipe saved = recipeRepository.save(recipe);
        return recipeMapper.toResponse(saved);
    }

    @Override
    public RecipeResponse getRecipeById(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe không tồn tại: " + id));
        return recipeMapper.toResponse(recipe);
    }

    @Override
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe không tồn tại: " + id));

        recipeMapper.updateRecipeFromDto(request, recipe);

        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        Recipe updated = recipeRepository.save(recipe);
        return recipeMapper.toResponse(updated);
    }

    @Override
    public void deleteRecipe(UUID id) {
        if (!recipeRepository.existsById(id)) {
            throw new RuntimeException("Recipe không tồn tại: " + id);
        }
        recipeRepository.deleteById(id);
    }

    @Override
    public Page<RecipeResponse> getAllRecipes(Pageable pageable) {
        return recipeRepository.findAll(pageable)
                .map(recipeMapper::toResponse);
    }

    private String generateSlug(String title) {
        return title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
