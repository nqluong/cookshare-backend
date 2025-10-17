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

    // ================= CREATE =================
    @Override
    public RecipeResponse createRecipe(RecipeRequest request) {
        Recipe recipe = recipeMapper.toEntity(request);

        // ✅ Generate slug nếu chưa có
        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        Recipe saved = recipeRepository.save(recipe);

        // ✅ Lưu step, ingredient, tag, category
        saveRelations(saved, request);

        return getRecipeById(saved.getRecipeId());
    }

    // ================= READ =================
    @Override
    public RecipeResponse getRecipeById(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Không tìm thấy công thức"));

        RecipeResponse response = recipeMapper.toResponse(recipe);

        // ✅ Map Steps
        response.setSteps(
                stepRepository.findByRecipe_RecipeId(id)
                        .stream()
                        .map(s -> RecipeResponse.RecipeStepResponse.builder()
                                .stepNumber(s.getStepNumber())
                                .instruction(s.getInstruction())
                                .imageUrl(s.getImageUrl())
                                .videoUrl(s.getVideoUrl())
                                .estimatedTime(s.getEstimatedTime())
                                .tips(s.getTips())
                                .build())
                        .toList()
        );

        // ✅ Map Ingredients
        response.setIngredients(
                ingredientRepository.findByRecipe_RecipeId(id)
                        .stream()
                        .map(i -> RecipeResponse.RecipeIngredientResponse.builder()
                                .ingredientId(i.getIngredientId())
                                .quantity(i.getQuantity())
                                .unit(i.getUnit())
                                .notes(i.getNotes())
                                .build())
                        .toList()
        );

        return response;
    }

    // ================= UPDATE =================
    @Override
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Không tìm thấy công thức"));

        recipeMapper.updateRecipeFromDto(request, recipe);
        Recipe updated = recipeRepository.save(recipe);

        // ✅ Xóa hết dữ liệu liên kết cũ
        stepRepository.deleteAll(stepRepository.findByRecipe_RecipeId(id));
        ingredientRepository.deleteAll(ingredientRepository.findByRecipe_RecipeId(id));
        tagRepository.deleteAll(tagRepository.findByRecipe_RecipeId(id));
        categoryRepository.deleteAll(categoryRepository.findByRecipe_RecipeId(id));

        // ✅ Lưu lại liên kết mới
        saveRelations(updated, request);

        return getRecipeById(updated.getRecipeId());
    }

    // ================= DELETE =================
    @Override
    public void deleteRecipe(UUID id) {
        if (!recipeRepository.existsById(id)) {
            throw new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Không tìm thấy công thức để xóa");
        }

        stepRepository.deleteAll(stepRepository.findByRecipe_RecipeId(id));
        ingredientRepository.deleteAll(ingredientRepository.findByRecipe_RecipeId(id));
        tagRepository.deleteAll(tagRepository.findByRecipe_RecipeId(id));
        categoryRepository.deleteAll(categoryRepository.findByRecipe_RecipeId(id));

        recipeRepository.deleteById(id);
    }

    // ================= USER RECIPES =================
    @Override
    public List<RecipeResponse> getRecipesByUserId(UUID userId) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId);
        if (recipes.isEmpty()) {
            throw new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Người dùng này chưa có công thức nào.");
        }
        return recipeMapper.toResponseList(recipes);
    }

    // ================= PAGINATION =================
    @Override
    public Page<RecipeResponse> getAllRecipes(Pageable pageable) {
        return recipeRepository.findAll(pageable).map(recipeMapper::toResponse);
    }

    // ================= HELPER METHODS =================
    private void saveRelations(Recipe recipe, RecipeRequest request) {
        UUID recipeId = recipe.getRecipeId();

        // ✅ Step
        if (request.getSteps() != null && !request.getSteps().isEmpty()) {
            List<RecipeStep> steps = recipeMapper.toStepEntities(request.getSteps(), recipe);
            stepRepository.saveAll(steps);
        }

        // ✅ Ingredient
        if (request.getIngredients() != null && !request.getIngredients().isEmpty()) {
            List<RecipeIngredient> ingredients = request.getIngredients().stream()
                    .map(dto -> RecipeIngredient.builder()
                            .recipeId(recipeId)
                            .ingredientId(dto.getIngredientId())
                            .quantity(dto.getQuantity())
                            .unit(dto.getUnit())
                            .notes(dto.getNotes())
                            .orderIndex(dto.getOrderIndex())
                            .build())
                    .toList();
            ingredientRepository.saveAll(ingredients);
        }

        // ✅ Tag
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<RecipeTag> tags = request.getTagIds().stream()
                    .map(tagId -> RecipeTag.builder()
                            .recipeId(recipeId)
                            .tagId(tagId)
                            .build())
                    .toList();
            tagRepository.saveAll(tags);
        }

        // ✅ Category
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<RecipeCategory> categories = request.getCategoryIds().stream()
                    .map(catId -> RecipeCategory.builder()
                            .recipeId(recipeId)
                            .categoryId(catId)
                            .build())
                    .toList();
            categoryRepository.saveAll(categories);
        }
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
