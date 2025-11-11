package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeDetailsResult;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

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
    private final FirebaseStorageService fileStorageService;

    // ================= CREATE =================

    @Override
    @Transactional
    public RecipeResponse createRecipeWithFiles(RecipeRequest request, MultipartFile image, List<MultipartFile> stepImages) {
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.uploadFile(image);
            request.setFeaturedImage(imageUrl);
        }

        if (request.getSteps() != null && stepImages != null) {
            for (int i = 0; i < Math.min(request.getSteps().size(), stepImages.size()); i++) {
                MultipartFile stepImage = stepImages.get(i);
                if (stepImage != null && !stepImage.isEmpty()) {
                    String stepImageUrl = fileStorageService.uploadFile(stepImage);
                    request.getSteps().get(i).setImageUrl(stepImageUrl);
                }
            }
        }

        return createRecipe(request);
    }

    @Override
    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request) {
        Recipe recipe = recipeMapper.toEntity(request);

        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());

        Recipe savedRecipe = recipeRepository.save(recipe);
        UUID recipeId = savedRecipe.getRecipeId();

        saveRecipeRelations(recipeId, request);
        return loadRecipeResponse(savedRecipe);
    }

    // ================= UPDATE =================

    @Override
    @Transactional
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request,
                                       MultipartFile image, List<MultipartFile> stepImages) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Không tìm thấy recipe id: " + id));

        if (image != null && !image.isEmpty()) {
            if (recipe.getFeaturedImage() != null) {
                fileStorageService.deleteFile(recipe.getFeaturedImage());
            }
            String newImageUrl = fileStorageService.uploadFile(image);
            request.setFeaturedImage(newImageUrl);
        }

        if (request.getSteps() != null && stepImages != null) {
            for (int i = 0; i < Math.min(request.getSteps().size(), stepImages.size()); i++) {
                MultipartFile stepImage = stepImages.get(i);
                if (stepImage != null && !stepImage.isEmpty()) {
                    String stepImageUrl = fileStorageService.uploadFile(stepImage);
                    request.getSteps().get(i).setImageUrl(stepImageUrl);
                }
            }
        }

        recipeMapper.updateRecipeFromDto(request, recipe);
        recipe.setUpdatedAt(LocalDateTime.now());

        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        Recipe updated = recipeRepository.save(recipe);

        recipeStepRepository.deleteAllByRecipeId(id);
        recipeIngredientRepository.deleteAllByRecipeId(id);
        recipeTagRepository.deleteAllByRecipeId(id);
        recipeCategoryRepository.deleteAllByRecipeId(id);

        saveRecipeRelations(id, request);

        return loadRecipeResponse(updated);
    }

    // ================= GET / DELETE =================

    @Override
    public RecipeResponse getRecipeById(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));
        return loadRecipeResponse(recipe);
    }

    @Override
    public void deleteRecipe(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        if (recipe.getFeaturedImage() != null) {
            fileStorageService.deleteFile(recipe.getFeaturedImage());
        }

        recipeRepository.deleteById(id);
    }

    @Override
    public Page<RecipeResponse> getAllRecipes(Pageable pageable) {
        return recipeRepository.findAll(pageable).map(recipeMapper::toResponse);
    }

    @Override
    public List<RecipeResponse> getAllRecipesByUserId(UUID userId) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId);
        if (recipes == null || recipes.isEmpty()) return Collections.emptyList();
        return recipes.stream().map(recipeMapper::toResponse).toList();
    }


    private RecipeResponse loadRecipeResponse(Recipe recipe) {
        RecipeDetailsResult details =
                recipeLoaderHelper.loadRecipeDetailsForPublic(recipe.getRecipeId(), recipe.getUserId());

        RecipeResponse response = recipeMapper.toResponse(recipe);
        response.setSteps(details.steps);
        response.setIngredients(details.ingredients);
        response.setTags(details.tags);
        response.setCategories(details.categories);
        response.setFullName(details.fullName);
        return response;
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private void saveRecipeRelations(UUID recipeId, RecipeRequest request) {
        if (request.getSteps() != null) {
            request.getSteps().forEach(step ->
                    recipeStepRepository.insertRecipeStep(
                            recipeId,
                            step.getStepNumber(),
                            step.getInstruction(),
                            step.getImageUrl(),
                            step.getVideoUrl(),
                            step.getEstimatedTime(),
                            step.getTips()
                    )
            );
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
