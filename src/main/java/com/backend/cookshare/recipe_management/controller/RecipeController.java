package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final ObjectMapper objectMapper;

    /**
     * [POST] /api/recipes
     * ➤ Tạo mới công thức (JSON + ảnh)
     */
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<RecipeResponse> createRecipe(
            @RequestPart("data") String data,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "stepImages", required = false) List<MultipartFile> stepImages
    ) throws IOException {

        // ✅ Convert JSON trong form-data sang object RecipeRequest
        RecipeRequest request = objectMapper.readValue(data, RecipeRequest.class);

        RecipeResponse response = recipeService.createRecipeWithFiles(request, image, stepImages);
        return ResponseEntity.ok(response);
    }

    /**
     * [GET] /api/recipes/user/{userId}
     * ➤ Lấy tất cả công thức theo userId
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecipeResponse>> getAllRecipesByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(recipeService.getAllRecipesByUserId(userId));
    }

    /**
     * [GET] /api/recipes/{id}
     * ➤ Lấy chi tiết công thức
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable UUID id) {
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    /**
     * [PUT] /api/recipes/{id}
     * ➤ Cập nhật công thức (chỉ JSON)
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable UUID id,
            @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, request));
    }

    /**
     * [DELETE] /api/recipes/{id}
     * ➤ Xóa công thức
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable UUID id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * [GET] /api/recipes?page=&size=
     * ➤ Phân trang danh sách công thức
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
