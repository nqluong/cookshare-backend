package com.backend.cookshare.recipe_management.service;

import com.backend.cookshare.recipe_management.dto.CategoryRequest;
import com.backend.cookshare.recipe_management.dto.response.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(UUID id, CategoryRequest request);
    void delete(UUID id);
    CategoryResponse getById(UUID id);
    List<CategoryResponse> getAll();
}
