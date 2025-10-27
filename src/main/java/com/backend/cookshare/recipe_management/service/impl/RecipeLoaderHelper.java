package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.recipe_management.dto.response.*;
import com.backend.cookshare.recipe_management.repository.RecipeCategoryRepository;
import com.backend.cookshare.recipe_management.repository.RecipeIngredientRepository;
import com.backend.cookshare.recipe_management.repository.RecipeStepRepository;
import com.backend.cookshare.recipe_management.repository.RecipeTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeLoaderHelper {
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeTagRepository recipeTagRepository;
    private final RecipeCategoryRepository recipeCategoryRepository;
    private final UserService userService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.min(Runtime.getRuntime().availableProcessors(), 4),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("recipe-loader-" + counter++);
                    thread.setDaemon(true);
                    return thread;
                }
            }
    );


    public CompletableFuture<List<RecipeStepResponse>> loadStepsAsync(UUID recipeId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return recipeStepRepository.findByRecipeIdOrderByStepNumber(recipeId);
                    } catch (Exception e) {
                        log.error("Lỗi khi load steps: {}", e.getMessage());
                        return Collections.emptyList();
                    }
                },
                executorService
        );
    }

    public CompletableFuture<List<RecipeIngredientResponse>> loadIngredientsAsync(UUID recipeId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return recipeIngredientRepository.findIngredientsByRecipeId(recipeId);
                    } catch (Exception e) {
                        log.error("Lỗi khi load ingredients: {}", e.getMessage());
                        return Collections.emptyList();
                    }
                },
                executorService
        );
    }

    public CompletableFuture<List<TagResponse>> loadTagsAsync(UUID recipeId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return recipeTagRepository.findTagsByRecipeId(recipeId);
                    } catch (Exception e) {
                        log.error("Lỗi khi load tags: {}", e.getMessage());
                        return Collections.emptyList();
                    }
                },
                executorService
        );
    }

    public CompletableFuture<List<CategoryResponse>> loadCategoriesAsync(UUID recipeId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return recipeCategoryRepository.findCategoriesByRecipeId(recipeId);
                    } catch (Exception e) {
                        log.error("Lỗi khi load categories: {}", e.getMessage());
                        return Collections.emptyList();
                    }
                },
                executorService
        );
    }

    public CompletableFuture<String> loadFullNameAsync(UUID userId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return userService.getUserById(userId)
                                .map(User::getFullName)
                                .orElse(null);
                    } catch (Exception e) {
                        log.error("Lỗi khi load username: {}", e.getMessage());
                        return null;
                    }
                },
                executorService
        );
    }

    public CompletableFuture<User> loadUserAsync(UUID userId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return userService.getUserById(userId).orElse(null);
                    } catch (Exception e) {
                        log.error("Lỗi khi load user: {}", e.getMessage());
                        return null;
                    }
                },
                executorService
        );
    }

    public RecipeDetailsResult loadRecipeDetailsForPublic(UUID recipeId, UUID userId) {
        try {
            CompletableFuture<String> usernameFuture = loadFullNameAsync(userId);
            CompletableFuture<List<RecipeStepResponse>> stepsFuture = loadStepsAsync(recipeId);
            CompletableFuture<List<RecipeIngredientResponse>> ingredientsFuture = loadIngredientsAsync(recipeId);
            CompletableFuture<List<TagResponse>> tagsFuture = loadTagsAsync(recipeId);
            CompletableFuture<List<CategoryResponse>> categoriesFuture = loadCategoriesAsync(recipeId);

            CompletableFuture.allOf(
                    usernameFuture, stepsFuture, ingredientsFuture, tagsFuture, categoriesFuture
            ).get(5, TimeUnit.SECONDS);

            RecipeDetailsResult result = new RecipeDetailsResult();
            result.fullName = usernameFuture.get();
            result.steps = stepsFuture.get();
            result.ingredients = ingredientsFuture.get();
            result.tags = tagsFuture.get();
            result.categories = categoriesFuture.get();

            log.info("Đã load recipe {} với {} steps, {} ingredients, {} tags, {} categories",
                    recipeId, result.steps.size(), result.ingredients.size(),
                    result.tags.size(), result.categories.size());

            return result;

        } catch (Exception e) {
            log.error("Lỗi khi load recipe details: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi load recipe details", e);
        }
    }


    public RecipeDetailsResult loadRecipeDetailsForAdmin(UUID recipeId, UUID userId) {
        try {
            CompletableFuture<User> userFuture = loadUserAsync(userId);
            CompletableFuture<List<RecipeStepResponse>> stepsFuture = loadStepsAsync(recipeId);
            CompletableFuture<List<RecipeIngredientResponse>> ingredientsFuture = loadIngredientsAsync(recipeId);
            CompletableFuture<List<TagResponse>> tagsFuture = loadTagsAsync(recipeId);
            CompletableFuture<List<CategoryResponse>> categoriesFuture = loadCategoriesAsync(recipeId);

            CompletableFuture.allOf(
                    userFuture, stepsFuture, ingredientsFuture, tagsFuture, categoriesFuture
            ).get(5, TimeUnit.SECONDS);

            RecipeDetailsResult result = new RecipeDetailsResult();
            result.user = userFuture.get();
            result.steps = stepsFuture.get();
            result.ingredients = ingredientsFuture.get();
            result.tags = tagsFuture.get();
            result.categories = categoriesFuture.get();

            log.info("Đã load recipe {} với {} steps, {} ingredients, {} tags, {} categories",
                    recipeId, result.steps.size(), result.ingredients.size(),
                    result.tags.size(), result.categories.size());

            return result;

        } catch (Exception e) {
            log.error("Lỗi khi load recipe details cho admin: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi load recipe details", e);
        }
    }
}
