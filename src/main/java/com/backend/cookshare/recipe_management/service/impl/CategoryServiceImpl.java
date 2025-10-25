package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.CategoryRequest;
import com.backend.cookshare.recipe_management.dto.response.CategoryResponse;
import com.backend.cookshare.recipe_management.entity.Category;
import com.backend.cookshare.recipe_management.mapper.CategoryMapper;
import com.backend.cookshare.recipe_management.repository.CategoryRepository;
import com.backend.cookshare.recipe_management.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        // 🔍 Kiểm tra trùng tên (không phân biệt hoa thường)
        categoryRepository.findByName(request.getName()).ifPresent(c -> {
            throw new CustomException(ErrorCode.CATEGORY_ALREADY_EXISTS, "Danh mục đã tồn tại");
        });

        // 🆕 Tạo mới
        Category category = categoryMapper.toEntity(request);
        category.setSlug(generateSlug(request.getName()));
        category.setCreatedAt(LocalDateTime.now());

        // 💾 Lưu và trả về response
        return categoryMapper.toResponse(categoryRepository.save(category));
    }


    @Override
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        categoryMapper.updateEntity(category, request);
        categoryRepository.save(category);
        return categoryMapper.toResponse(category);
    }

    @Override
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse getById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        return categoryMapper.toResponse(category);
    }

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    // Helper to create URL-friendly slugs (removes diacritics, punctuation, collapses spaces)
    private String generateSlug(String input) {
        if (input == null) return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
        return slug;
    }
}
