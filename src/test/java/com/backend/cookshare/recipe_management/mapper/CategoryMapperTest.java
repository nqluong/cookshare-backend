package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.request.CategoryRequest;
import com.backend.cookshare.recipe_management.dto.response.CategoryResponse;
import com.backend.cookshare.recipe_management.entity.Category;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryMapperTest {

    private final CategoryMapper mapper = Mappers.getMapper(CategoryMapper.class);

    // =========================
    // toEntity
    // =========================
    @Test
    void toEntity_fullMapping() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Dessert");
        request.setSlug("dessert");
        request.setDescription("Sweet food");

        Category entity = mapper.toEntity(request);

        assertNotNull(entity);
        assertEquals("Dessert", entity.getName());
        assertEquals("dessert", entity.getSlug());
        assertEquals("Sweet food", entity.getDescription());
    }

    // =========================
    // toResponse
    // =========================
    @Test
    void toResponse_fullMapping() {
        Category category = new Category();
        category.setCategoryId(UUID.randomUUID());
        category.setName("Main Dish");
        category.setSlug("main-dish");
        category.setDescription("Main course");

        CategoryResponse response = mapper.toResponse(category);

        assertNotNull(response);
        assertEquals("Main Dish", response.getName());
        assertEquals("main-dish", response.getSlug());
        assertEquals("Main course", response.getDescription());
    }

    // =========================
    // updateEntity
    // =========================
    @Test
    void updateEntity_updatesExistingEntity() {
        Category category = new Category();
        category.setCategoryId(UUID.randomUUID());
        category.setName("Old");
        category.setSlug("old");
        category.setDescription("Old desc");

        CategoryRequest request = new CategoryRequest();
        request.setName("New");
        request.setSlug("new");
        request.setDescription("New desc");

        mapper.updateEntity(category, request);

        assertEquals("New", category.getName());
        assertEquals("new", category.getSlug());
        assertEquals("New desc", category.getDescription());
    }

    // =========================
    // edge cases
    // =========================
    @Test
    void toResponse_nullInput() {
        CategoryResponse res = mapper.toResponse(null);
        assertNull(res);
    }

    @Test
    void toEntity_nullInput() {
        Category entity = mapper.toEntity(null);
        assertNull(entity);
    }
}
