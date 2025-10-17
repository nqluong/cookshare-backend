package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.recipe_management.dto.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.RecipeResponse;
import com.backend.cookshare.recipe_management.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cung cấp các API CRUD cho Recipe
 */
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * [POST] /api/recipes
     * ➤ Tạo mới công thức nấu ăn
     */
    @PostMapping
    public ResponseEntity<RecipeResponse> createRecipe(@Valid @RequestBody RecipeRequest request) {
        RecipeResponse response = recipeService.createRecipe(request);
        return ResponseEntity.ok(response);
    }

    /**
     * [GET] /api/recipes/{id}
     * ➤ Lấy chi tiết công thức theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable UUID id) {
        RecipeResponse response = recipeService.getRecipeById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * [PUT] /api/recipes/{id}
     * ➤ Cập nhật công thức hiện có
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable UUID id,
            @Valid @RequestBody RecipeRequest request
    ) {
        RecipeResponse response = recipeService.updateRecipe(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * [DELETE] /api/recipes/{id}
     * ➤ Xóa công thức theo ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable UUID id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * [GET] /api/recipes?page=0&size=10
     * ➤ Lấy danh sách công thức có phân trang
     */
    @GetMapping
    public ResponseEntity<PageResponse<RecipeResponse>> getAllRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<RecipeResponse> recipePage = recipeService.getAllRecipes(PageRequest.of(page, size));

        PageResponse<RecipeResponse> response = PageResponse.<RecipeResponse>builder()
                .content(recipePage.getContent())
                .page(recipePage.getNumber())
                .size(recipePage.getSize())
                .totalElements(recipePage.getTotalElements())
                .totalPages(recipePage.getTotalPages())
                .first(recipePage.isFirst())
                .last(recipePage.isLast())
                .empty(recipePage.isEmpty())
                .numberOfElements(recipePage.getNumberOfElements())
                .sorted(recipePage.getSort().isSorted())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * [GET] /api/recipes/user/{userId}
     * ➤ Lấy danh sách công thức theo ID người dùng
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecipeResponse>> getAllRecipesByUserId(@PathVariable UUID userId) {
        List<RecipeResponse> recipes = recipeService.getAllRecipesByUserId(userId);
        return ResponseEntity.ok(recipes);
    }
}
