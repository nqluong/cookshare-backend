package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.dto.response.*;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.mapper.RecipeMapper;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.recipe_management.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    /**
     * ✅ Tạo công thức mới
     */
    @Override
    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request) {
        // 1️⃣ Map Recipe chính
        Recipe recipe = recipeMapper.toEntity(request);

        // Nếu chưa có slug → tự sinh từ title
        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());

        // 2️⃣ Lưu recipe chính
        Recipe savedRecipe = recipeRepository.save(recipe);
        UUID recipeId = savedRecipe.getRecipeId();

        // 3️⃣ Lưu các bảng phụ (step, ingredient, tag, category)
        saveRecipeRelations(recipeId, request);

        // 4️⃣ Truy vấn lại toàn bộ chi tiết (native query)
        List<Object[]> rows = recipeRepository.findRecipeDetailsById(recipeId);

        return mapRecipeDetails(savedRecipe, rows);
    }

    /**
     * ✅ Lấy chi tiết công thức có đủ step, ingredient, tag, category
     */
    @Override
    public RecipeResponse getRecipeById(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND,
                        "Không tìm thấy recipe id: " + id));

        List<Object[]> rows = recipeRepository.findRecipeDetailsById(id);
        return mapRecipeDetails(recipe, rows);
    }

    /**
     * ✅ Cập nhật công thức
     */
    @Override
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND,
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
     * ✅ Lấy danh sách công thức (phân trang)
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

    // ============================================================
    // 🔹 PRIVATE SUPPORT METHODS
    // ============================================================

    /**
     * Lưu các quan hệ phụ cho công thức
     */
    private void saveRecipeRelations(UUID recipeId, RecipeRequest request) {
        if (request.getSteps() != null) {
            request.getSteps().forEach(step -> recipeRepository.insertRecipeStep(
                    recipeId,
                    step.getStepNumber(),
                    step.getInstruction(),
                    step.getImageUrl(),
                    step.getVideoUrl(),
                    step.getEstimatedTime(),
                    step.getTips()
            ));
        }

        if (request.getIngredients() != null) {
            request.getIngredients().forEach(ingredientId ->
                    recipeRepository.insertRecipeIngredient(recipeId, ingredientId)
            );
        }

        if (request.getTagIds() != null) {
            request.getTagIds().forEach(tagId ->
                    recipeRepository.insertRecipeTag(recipeId, tagId)
            );
        }

        if (request.getCategoryIds() != null) {
            request.getCategoryIds().forEach(categoryId ->
                    recipeRepository.insertRecipeCategory(recipeId, categoryId)
            );
        }
    }

    /**
     * Map dữ liệu chi tiết recipe (từ native query)
     */
    private RecipeResponse mapRecipeDetails(Recipe recipe, List<Object[]> rows) {
        RecipeResponse response = recipeMapper.toResponse(recipe);

        Map<Integer, RecipeStepResponse> stepMap = new LinkedHashMap<>();
        Map<UUID, RecipeIngredientResponse> ingredientMap = new LinkedHashMap<>();
        Map<UUID, TagResponse> tagMap = new LinkedHashMap<>();
        Map<UUID, CategoryResponse> categoryMap = new LinkedHashMap<>();

        for (Object[] r : rows) {
            // ---------- Step ----------
            Integer stepNumber = (Integer) r[0];
            if (stepNumber != null && !stepMap.containsKey(stepNumber)) {
                stepMap.put(stepNumber, RecipeStepResponse.builder()
                        .stepNumber(stepNumber)
                        .instruction((String) r[1])
                        .imageUrl((String) r[2])
                        .videoUrl((String) r[3])
                        .estimatedTime((Integer) r[4])
                        .tips((String) r[5])
                        .build());
            }

            // ---------- Ingredient ----------
            if (r[6] != null) {
                UUID ingredientId = (UUID) r[6];
                if (!ingredientMap.containsKey(ingredientId)) {
                    LocalDateTime ingredientCreatedAt = null;
                    if (r[11] instanceof Timestamp ts) ingredientCreatedAt = ts.toLocalDateTime();

                    ingredientMap.put(ingredientId, RecipeIngredientResponse.builder()
                            .ingredientId(ingredientId)
                            .name((String) r[7])
                            .slug((String) r[8])
                            .description((String) r[9])
                            .category((String) r[10])
                            .createdAt(ingredientCreatedAt)
                            .quantity((String) r[12])
                            .unit((String) r[13])
                            .notes((String) r[14])
                            .orderIndex((Integer) r[15])
                            .build());
                }
            }

            // ---------- Tag ----------
            if (r[16] != null) {
                UUID tagId = (UUID) r[16];
                if (!tagMap.containsKey(tagId)) {
                    LocalDateTime tagCreatedAt = null;
                    if (r[22] instanceof Timestamp ts) tagCreatedAt = ts.toLocalDateTime();

                    tagMap.put(tagId, TagResponse.builder()
                            .tagId(tagId)
                            .name((String) r[17])
                            .slug((String) r[18])
                            .color((String) r[19])
                            .usageCount((Integer) r[20])
                            .isTrending((Boolean) r[21])
                            .createdAt(tagCreatedAt)
                            .build());
                }
            }

            // ---------- Category ----------
            if (r[23] != null) {
                UUID categoryId = (UUID) r[23];
                if (!categoryMap.containsKey(categoryId)) {
                    LocalDateTime categoryCreatedAt = null;
                    if (r[30] instanceof Timestamp ts) categoryCreatedAt = ts.toLocalDateTime();

                    categoryMap.put(categoryId, CategoryResponse.builder()
                            .categoryId(categoryId)
                            .name((String) r[24])
                            .slug((String) r[25])
                            .description((String) r[26])
                            .iconUrl((String) r[27])
                            .parentId((UUID) r[28])
                            .isActive((Boolean) r[29])
                            .createdAt(categoryCreatedAt)
                            .build());
                }
            }
        }

        // Gắn dữ liệu vào response cuối
        response.setSteps(new ArrayList<>(stepMap.values()));
        response.setIngredients(new ArrayList<>(ingredientMap.values()));
        response.setTags(new ArrayList<>(tagMap.values()));
        response.setCategories(new ArrayList<>(categoryMap.values()));

        return response;
    }

    /**
     * Sinh slug từ tiêu đề
     */
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
