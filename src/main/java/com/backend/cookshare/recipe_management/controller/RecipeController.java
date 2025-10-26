package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * [POST] /api/recipes
     * ➤ Tạo mới một công thức nấu ăn
     */
    @PostMapping
    public ResponseEntity<RecipeResponse> createRecipe(@Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.createRecipe(request));
    }

    /**
     * [GET] /api/recipes/user/{userId}
     * ➤ Lấy tất cả công thức theo userId
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecipeResponse>> getAllRecipesByUserId(@PathVariable UUID userId) {
        List<RecipeResponse> responses = recipeService.getAllRecipesByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * [GET] /api/recipes/{id}
     * ➤ Lấy chi tiết một công thức theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable UUID id) {
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    /**g
     * [PUT] /api/recipes/{id}
     * ➤ Cập nhật thông tin công thức theo ID
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> updateRecipe(@PathVariable UUID id, @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, request));
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
     * [GET] /api/recipes
     * ➤ Lấy danh sách tất cả công thức (có phân trang)
     */
    @GetMapping
    public ResponseEntity<PageResponse<RecipeResponse>> getAllRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

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
                .build();

        return ResponseEntity.ok(response);
    }
}
