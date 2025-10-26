package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * [POST] /api/recipes
     * ➤ Tạo mới công thức (1 API duy nhất, gồm JSON + ảnh)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecipeResponse> createRecipe(
            @RequestPart("data") @Valid RecipeRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "stepImages", required = false) List<MultipartFile> stepImages
    ) {
        return ResponseEntity.ok(recipeService.createRecipeWithFiles(request, image, stepImages));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecipeResponse>> getAllRecipesByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(recipeService.getAllRecipesByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable UUID id) {
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable UUID id,
            @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable UUID id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }

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
