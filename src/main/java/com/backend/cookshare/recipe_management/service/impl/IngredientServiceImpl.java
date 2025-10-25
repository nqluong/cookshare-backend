package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import com.backend.cookshare.recipe_management.mapper.IngredientMapper;
import com.backend.cookshare.recipe_management.repository.IngredientRepository;
import com.backend.cookshare.recipe_management.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;

    @Override
    @Transactional
    public RecipeIngredientResponse createIngredient(IngredientRequest request) {
        // Kiểm tra tồn tại
        ingredientRepository.findByNameIgnoreCase(request.getName()).ifPresent(i -> {
            throw new CustomException(ErrorCode.TAG_ALREADY_EXISTS, "Nguyên liệu đã tồn tại");
        });

        Ingredient ingredient = ingredientMapper.toEntity(request);
        ingredient.setSlug(generateSlug(request.getName()));
        ingredient.setCreatedAt(LocalDateTime.now());
        ingredient.setUsageCount(0);

        return ingredientMapper.toResponse(ingredientRepository.save(ingredient));
    }

    @Override
    public RecipeIngredientResponse getIngredientById(UUID id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "Không tìm thấy nguyên liệu"));
        return ingredientMapper.toResponse(ingredient);
    }

    @Override
    @Transactional
    public RecipeIngredientResponse updateIngredient(UUID id, IngredientRequest request) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "Không tìm thấy nguyên liệu"));

        ingredientMapper.updateIngredientFromDto(request, ingredient);
        ingredient.setSlug(generateSlug(ingredient.getName()));

        return ingredientMapper.toResponse(ingredientRepository.save(ingredient));
    }

    @Override
    @Transactional
    public void deleteIngredient(UUID id) {
        if (!ingredientRepository.existsById(id)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Không tìm thấy nguyên liệu để xóa");
        }
        ingredientRepository.deleteById(id);
    }

    @Override
    public List<RecipeIngredientResponse> getAllIngredients() {
        return ingredientRepository.findAll()
                .stream()
                .map(ingredientMapper::toResponse)
                .toList();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
