package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.entity.*;
import com.backend.cookshare.recipe_management.mapper.RecipeMapper;
import com.backend.cookshare.recipe_management.repository.*;
import com.backend.cookshare.recipe_management.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeStepRepository stepRepository;
    private final RecipeIngredientRepository ingredientRepository;
    private final RecipeTagRepository tagRepository;
    private final RecipeCategoryRepository categoryRepository;
    private final RecipeMapper recipeMapper;


    @Override
    public RecipeResponse createRecipe(RecipeRequest request) {
        Recipe recipe = recipeMapper.toEntity(request);

        // Generate slug
        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        Recipe saved = recipeRepository.save(recipe);

        // ✅ Lưu step, ingredient, tag, category
        saveRelations(saved.getRecipeId(), request);

        return getRecipeById(saved.getRecipeId());
    }

    @Override
    public RecipeResponse getRecipeById(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Không tìm thấy công thức"));

        RecipeResponse response = recipeMapper.toResponse(recipe);

        // ✅ Lấy dữ liệu liên quan
        response.setSteps(stepRepository.findByRecipeId(id).stream().map(
                s -> RecipeResponse.RecipeStepResponse.builder()
                        .stepNumber(s.getStepNumber())
                        .instruction(s.getInstruction())
                        .imageUrl(s.getImageUrl())
                        .videoUrl(s.getVideoUrl())
                        .estimatedTime(s.getEstimatedTime())
                        .tips(s.getTips())
                        .build()
        ).toList());

        response.setIngredients(ingredientRepository.findByRecipeId(id).stream().map(
                i -> RecipeResponse.RecipeIngredientResponse.builder()
                        .ingredientId(i.getIngredientId())
                        .quantity(i.getQuantity())
                        .unit(i.getUnit())
                        .notes(i.getNotes())
                        .build()
        ).toList());

        return response;
    }

    @Override
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Không tìm thấy công thức"));

        recipeMapper.updateRecipeFromDto(request, recipe);
        Recipe updated = recipeRepository.save(recipe);

        // ✅ Cập nhật lại liên kết
        stepRepository.deleteAll(stepRepository.findByRecipeId(id));
        ingredientRepository.deleteAll(ingredientRepository.findByRecipeId(id));
        tagRepository.deleteAll(tagRepository.findByRecipeId(id));
        categoryRepository.deleteAll(categoryRepository.findByRecipeId(id));

        saveRelations(id, request);

        return getRecipeById(updated.getRecipeId());
    }
    @Override
    public List<RecipeResponse> getRecipesByUserId(UUID userId) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId);
        if (recipes.isEmpty()) {
            throw new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Người dùng này chưa có công thức nào.");
        }
        return recipeMapper.toResponseList(recipes);
    }

    @Override
    public void deleteRecipe(UUID id) {
        if (!recipeRepository.existsById(id)) {
            throw new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Không tìm thấy công thức để xóa");
        }

        stepRepository.deleteAll(stepRepository.findByRecipeId(id));
        ingredientRepository.deleteAll(ingredientRepository.findByRecipeId(id));
        tagRepository.deleteAll(tagRepository.findByRecipeId(id));
        categoryRepository.deleteAll(categoryRepository.findByRecipeId(id));

        recipeRepository.deleteById(id);
    }

    @Override
    public Page<RecipeResponse> getAllRecipes(Pageable pageable) {
        return recipeRepository.findAll(pageable)
                .map(recipeMapper::toResponse);
    }

    private void saveRelations(UUID recipeId, RecipeRequest request) {
        if (request.getSteps() != null) {
            List<RecipeStep> steps = recipeMapper.toStepEntities(request.getSteps(), recipeId);
            stepRepository.saveAll(steps);
        }
        if (request.getIngredients() != null) {
            List<RecipeIngredient> ingredients = recipeMapper.toIngredientEntities(request.getIngredients(), recipeId);
            ingredientRepository.saveAll(ingredients);
        }
        if (request.getTagIds() != null) {
            tagRepository.saveAll(request.getTagIds().stream()
                    .map(tagId -> RecipeTag.builder().recipeId(recipeId).tagId(tagId).build())
                    .toList());
        }
        if (request.getCategoryIds() != null) {
            categoryRepository.saveAll(request.getCategoryIds().stream()
                    .map(catId -> RecipeCategory.builder().recipeId(recipeId).categoryId(catId).build())
                    .toList());
        }
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
