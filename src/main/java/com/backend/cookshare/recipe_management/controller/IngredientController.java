package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @PostMapping
    public ResponseEntity<RecipeIngredientResponse> create(@RequestBody IngredientRequest request) {
        return ResponseEntity.ok(ingredientService.createIngredient(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeIngredientResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ingredientService.getIngredientById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeIngredientResponse> update(@PathVariable UUID id,
                                                           @RequestBody IngredientRequest request) {
        return ResponseEntity.ok(ingredientService.updateIngredient(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ingredientService.deleteIngredient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<RecipeIngredientResponse>> getAll() {
        return ResponseEntity.ok(ingredientService.getAllIngredients());
    }
}
