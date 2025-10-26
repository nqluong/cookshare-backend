package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.dto.response.RecipeDetailsResult;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.mapper.RecipeMapper;
import com.backend.cookshare.recipe_management.repository.*;
import com.backend.cookshare.recipe_management.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeTagRepository recipeTagRepository;
    private final RecipeCategoryRepository recipeCategoryRepository;
    private final RecipeMapper recipeMapper;
    private final RecipeLoaderHelper recipeLoaderHelper;


    /**
     * ✅ Tạo công thức mới (chỉ lưu công thức chính)
     */
    @Override
    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request) {
        // 1️⃣ Map Recipe chính từ request
        Recipe recipe = recipeMapper.toEntity(request);

        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }
        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());

        // Lưu recipe chính
        Recipe savedRecipe = recipeRepository.save(recipe);
        UUID recipeId = savedRecipe.getRecipeId();

        // Lưu các bảng phụ
        saveRecipeRelations(recipeId, request);

        // Lấy lại chi tiết đầy đủ
        RecipeDetailsResult details =
                recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, savedRecipe.getUserId());

        RecipeResponse response = recipeMapper.toResponse(savedRecipe);
        response.setSteps(details.steps);
        response.setIngredients(details.ingredients);
        response.setTags(details.tags);
        response.setCategories(details.categories);
        response.setFullName(details.fullName);
        return response;
    }

    /**
     * ✅ Lấy chi tiết công thức bằng native query (có step, ingredient, tag, category)
     */
    @Override
    public RecipeResponse getRecipeById(UUID id) {
        log.info("Đang lấy chi tiết recipe: {}", id);

        // Load recipe trước (trong transaction)
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RECIPE_NOT_FOUND,
                        "Không tìm thấy recipe id: " + id));

        // Kiểm tra recipe có được publish không
        if (!recipe.getIsPublished()) {
            throw new CustomException(ErrorCode.RECIPE_NOT_PUBLISHED);
        }

        try {
            RecipeDetailsResult details =
                    recipeLoaderHelper.loadRecipeDetailsForPublic(id, recipe.getUserId());

            RecipeResponse response = recipeMapper.toResponse(recipe);
            response.setSteps(details.steps);
            response.setIngredients(details.ingredients);
            response.setTags(details.tags);
            response.setCategories(details.categories);
            response.setFullName(details.fullName);

            return response;

        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết recipe: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * ✅ Cập nhật công thức
     */
    @Override
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RECIPE_NOT_FOUND,
                        "Recipe không tồn tại với id: " + id));

        recipeMapper.updateRecipeFromDto(request, recipe);
        recipe.setUpdatedAt(LocalDateTime.now());

        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        Recipe updated = recipeRepository.save(recipe);
        return recipeMapper.toResponse(updated);
    }

    /**
     * ✅ Xóa công thức
     */
    @Override
    public void deleteRecipe(UUID id) {
        if (!recipeRepository.existsById(id)) {
            throw new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Recipe không tồn tại với id: " + id);
        }
        recipeRepository.deleteById(id);
    }

    /**
     * ✅ Lấy danh sách tất cả công thức (phân trang)
     */
    @Override
    public Page<RecipeResponse> getAllRecipes(Pageable pageable) {
        return recipeRepository.findAll(pageable).map(recipeMapper::toResponse);
    }

    /**
     * ✅ Lấy tất cả công thức theo user
     */
    @Override
    public List<RecipeResponse> getAllRecipesByUserId(UUID userId) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId);
        if (recipes == null || recipes.isEmpty()) return Collections.emptyList();
        return recipes.stream().map(recipeMapper::toResponse).toList();
    }

    /**
     * 🔹 Sinh slug từ tiêu đề
     */
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private void saveRecipeRelations(UUID recipeId, RecipeRequest request) {
        if (request.getSteps() != null) {
            request.getSteps().forEach(step -> {
                recipeStepRepository.insertRecipeStep(
                        recipeId,
                        step.getStepNumber(),
                        step.getInstruction(),
                        step.getImageUrl(),
                        step.getVideoUrl(),
                        step.getEstimatedTime(),
                        step.getTips()
                );
            });
        }

        if (request.getIngredients() != null) {
            request.getIngredients().forEach(ingredientId ->
                    recipeIngredientRepository.insertRecipeIngredient(recipeId, ingredientId)
            );
        }

        if (request.getTagIds() != null) {
            request.getTagIds().forEach(tagId ->
                    recipeTagRepository.insertRecipeTag(recipeId, tagId)
            );
        }

        if (request.getCategoryIds() != null) {
            request.getCategoryIds().forEach(categoryId ->
                    recipeCategoryRepository.insertRecipeCategory(recipeId, categoryId)
            );
        }
    }
}
