package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


    @PostMapping(value = "", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<RecipeResponse> createRecipe(
            @RequestPart("data") String data,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "stepImages", required = false) List<MultipartFile> stepImages) throws IOException {
        RecipeRequest request = objectMapper.readValue(data, RecipeRequest.class);
        RecipeResponse response = recipeService.createRecipeWithFiles(request, image, stepImages);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable UUID id,
            @RequestPart("data") String data,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "stepImages", required = false) List<MultipartFile> stepImages) throws IOException {
        RecipeRequest request = objectMapper.readValue(data, RecipeRequest.class);
        RecipeResponse response = recipeService.updateRecipe(id, request, image, stepImages);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable UUID id) {
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    @GetMapping
    public ResponseEntity<Page<RecipeResponse>> getAllRecipes(Pageable pageable) {
        return ResponseEntity.ok(recipeService.getAllRecipes(pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecipeResponse>> getAllRecipesByUser(
            @PathVariable UUID userId,
            @RequestParam(required = false) UUID currentUserId,
            @RequestParam(required = false, defaultValue = "false") boolean includeAll) {
        return ResponseEntity.ok(recipeService.getAllRecipesByUserId(userId, currentUserId, includeAll));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable UUID id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/toggle-privacy")
    public ResponseEntity<RecipeResponse> togglePrivacy(@PathVariable UUID id) {
        return ResponseEntity.ok(recipeService.togglePrivacy(id));
    }
}
