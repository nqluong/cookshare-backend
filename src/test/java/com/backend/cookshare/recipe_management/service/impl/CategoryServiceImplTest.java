package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.request.CategoryRequest;
import com.backend.cookshare.recipe_management.dto.response.CategoryResponse;
import com.backend.cookshare.recipe_management.entity.Category;
import com.backend.cookshare.recipe_management.mapper.CategoryMapper;
import com.backend.cookshare.recipe_management.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private UUID testId;
    private CategoryRequest categoryRequest;
    private Category category;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        categoryRequest = new CategoryRequest();
        categoryRequest.setName("Vietnamese Cuisine");
        categoryRequest.setDescription("Traditional Vietnamese dishes");

        category = new Category();
        category.setCategoryId(testId);
        category.setName("Vietnamese Cuisine");
        category.setSlug("vietnamese-cuisine");
        category.setDescription("Traditional Vietnamese dishes");
        category.setCreatedAt(LocalDateTime.now());

        categoryResponse = new CategoryResponse();
        categoryResponse.setCategoryId(testId);
        categoryResponse.setName("Vietnamese Cuisine");
        categoryResponse.setSlug("vietnamese-cuisine");
        categoryResponse.setDescription("Traditional Vietnamese dishes");
    }

    @Nested
    @DisplayName("Create Category Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create category successfully")
        void shouldCreateCategorySuccessfully() {
            // Given
            when(categoryRepository.findByName(categoryRequest.getName()))
                    .thenReturn(Optional.empty());
            when(categoryMapper.toEntity(categoryRequest)).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            // When
            CategoryResponse result = categoryService.create(categoryRequest);

            // Then
            assertNotNull(result);
            assertEquals(categoryResponse.getCategoryId(), result.getCategoryId());
            assertEquals(categoryResponse.getName(), result.getName());
            assertEquals(categoryResponse.getSlug(), result.getSlug());

            ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(categoryCaptor.capture());

            Category savedCategory = categoryCaptor.getValue();
            assertNotNull(savedCategory.getSlug());
            assertNotNull(savedCategory.getCreatedAt());

            verify(categoryRepository).findByName(categoryRequest.getName());
            verify(categoryMapper).toEntity(categoryRequest);
            verify(categoryMapper).toResponse(category);
        }

        @Test
        @DisplayName("Should throw exception when category already exists")
        void shouldThrowExceptionWhenCategoryAlreadyExists() {
            // Given
            when(categoryRepository.findByName(categoryRequest.getName()))
                    .thenReturn(Optional.of(category));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> categoryService.create(categoryRequest));

            assertEquals(ErrorCode.CATEGORY_ALREADY_EXISTS, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Danh mục đã tồn tại"));

            verify(categoryRepository).findByName(categoryRequest.getName());
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should generate correct slug for simple name")
        void shouldGenerateCorrectSlugForSimpleName() {
            // Given
            categoryRequest.setName("Italian Food");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            assertTrue(slug.matches("[a-z0-9-]+"));
            assertFalse(slug.contains(" "));
        }

        @Test
        @DisplayName("Should handle Vietnamese diacritics in slug generation")
        void shouldHandleVietnameseDiacriticsInSlugGeneration() {
            // Given
            categoryRequest.setName("Món Ăn Việt Nam");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            assertFalse(slug.contains("ă"));
            assertFalse(slug.contains("ê"));
            assertTrue(slug.matches("[a-z0-9-]+"));
        }

        @Test
        @DisplayName("Should handle special characters and punctuation")
        void shouldHandleSpecialCharactersAndPunctuation() {
            // Given
            categoryRequest.setName("Fast & Quick! Meals @ Home");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            assertFalse(slug.contains("&"));
            assertFalse(slug.contains("!"));
            assertFalse(slug.contains("@"));
            assertTrue(slug.matches("[a-z0-9-]+"));
        }

        @Test
        @DisplayName("Should handle multiple spaces in category name")
        void shouldHandleMultipleSpacesInCategoryName() {
            // Given
            categoryRequest.setName("Thai    Food    Recipes");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            assertFalse(slug.contains("  "));
        }

        @Test
        @DisplayName("Should handle leading and trailing spaces")
        void shouldHandleLeadingAndTrailingSpaces() {
            // Given
            categoryRequest.setName("  Desserts  ");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            assertFalse(slug.startsWith(" "));
            assertFalse(slug.endsWith(" "));
        }

        @Test
        @DisplayName("Should handle uppercase letters in slug")
        void shouldHandleUppercaseLettersInSlug() {
            // Given
            categoryRequest.setName("BREAKFAST RECIPES");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            assertEquals(slug, slug.toLowerCase());
        }

        @Test
        @DisplayName("Should handle numbers in category name")
        void shouldHandleNumbersInCategoryName() {
            // Given
            categoryRequest.setName("30 Minute Meals");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            assertTrue(slug.contains("30"));
        }

        @Test
        @DisplayName("Should handle hyphens in category name")
        void shouldHandleHyphensInCategoryName() {
            // Given
            categoryRequest.setName("Low-Carb Diet");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertNotNull(captor.getValue().getSlug());
        }
    }

    @Nested
    @DisplayName("Update Category Tests")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should update category successfully")
        void shouldUpdateCategorySuccessfully() {
            // Given
            CategoryRequest updateRequest = new CategoryRequest();
            updateRequest.setName("Updated Category");
            updateRequest.setDescription("Updated description");

            when(categoryRepository.findById(testId)).thenReturn(Optional.of(category));
            doNothing().when(categoryMapper).updateEntity(category, updateRequest);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            // When
            CategoryResponse result = categoryService.update(testId, updateRequest);

            // Then
            assertNotNull(result);
            assertEquals(categoryResponse.getCategoryId(), result.getCategoryId());

            verify(categoryRepository).findById(testId);
            verify(categoryMapper).updateEntity(category, updateRequest);
            verify(categoryRepository).save(category);
            verify(categoryMapper).toResponse(category);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent category")
        void shouldThrowExceptionWhenUpdatingNonExistentCategory() {
            // Given
            CategoryRequest updateRequest = new CategoryRequest();
            updateRequest.setName("Updated Name");

            when(categoryRepository.findById(testId)).thenReturn(Optional.empty());

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> categoryService.update(testId, updateRequest));

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());

            verify(categoryRepository).findById(testId);
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update category with partial information")
        void shouldUpdateCategoryWithPartialInformation() {
            // Given
            CategoryRequest updateRequest = new CategoryRequest();
            updateRequest.setName("New Name");

            when(categoryRepository.findById(testId)).thenReturn(Optional.of(category));
            doNothing().when(categoryMapper).updateEntity(category, updateRequest);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            // When
            CategoryResponse result = categoryService.update(testId, updateRequest);

            // Then
            assertNotNull(result);
            verify(categoryMapper).updateEntity(category, updateRequest);
        }
    }

    @Nested
    @DisplayName("Delete Category Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should delete category successfully")
        void shouldDeleteCategorySuccessfully() {
            // Given
            when(categoryRepository.findById(testId)).thenReturn(Optional.of(category));
            doNothing().when(categoryRepository).delete(category);

            // When
            categoryService.delete(testId);

            // Then
            verify(categoryRepository).findById(testId);
            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent category")
        void shouldThrowExceptionWhenDeletingNonExistentCategory() {
            // Given
            when(categoryRepository.findById(testId)).thenReturn(Optional.empty());

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> categoryService.delete(testId));

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());

            verify(categoryRepository).findById(testId);
            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should verify category entity is passed to delete")
        void shouldVerifyCategoryEntityIsPassedToDelete() {
            // Given
            when(categoryRepository.findById(testId)).thenReturn(Optional.of(category));
            doNothing().when(categoryRepository).delete(category);

            // When
            categoryService.delete(testId);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).delete(captor.capture());
            assertEquals(category.getCategoryId(), captor.getValue().getCategoryId());
        }
    }

    @Nested
    @DisplayName("Get Category By Id Tests")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Should get category by id successfully")
        void shouldGetCategoryByIdSuccessfully() {
            // Given
            when(categoryRepository.findById(testId)).thenReturn(Optional.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            // When
            CategoryResponse result = categoryService.getById(testId);

            // Then
            assertNotNull(result);
            assertEquals(categoryResponse.getCategoryId(), result.getCategoryId());
            assertEquals(categoryResponse.getName(), result.getName());
            assertEquals(categoryResponse.getSlug(), result.getSlug());

            verify(categoryRepository).findById(testId);
            verify(categoryMapper).toResponse(category);
        }

        @Test
        @DisplayName("Should throw exception when category not found by id")
        void shouldThrowExceptionWhenCategoryNotFoundById() {
            // Given
            when(categoryRepository.findById(testId)).thenReturn(Optional.empty());

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> categoryService.getById(testId));

            assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());

            verify(categoryRepository).findById(testId);
            verify(categoryMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should handle different UUID formats")
        void shouldHandleDifferentUUIDFormats() {
            // Given
            UUID randomId = UUID.randomUUID();
            when(categoryRepository.findById(randomId)).thenReturn(Optional.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            // When
            CategoryResponse result = categoryService.getById(randomId);

            // Then
            assertNotNull(result);
            verify(categoryRepository).findById(randomId);
        }
    }

    @Nested
    @DisplayName("Get All Categories Tests")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("Should get all categories successfully")
        void shouldGetAllCategoriesSuccessfully() {
            // Given
            Category category2 = new Category();
            category2.setCategoryId(UUID.randomUUID());
            category2.setName("Japanese Cuisine");
            category2.setSlug("japanese-cuisine");

            CategoryResponse response2 = new CategoryResponse();
            response2.setCategoryId(category2.getCategoryId());
            response2.setName("Japanese Cuisine");
            response2.setSlug("japanese-cuisine");

            List<Category> categories = Arrays.asList(category, category2);

            when(categoryRepository.findAll()).thenReturn(categories);
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);
            when(categoryMapper.toResponse(category2)).thenReturn(response2);

            // When
            List<CategoryResponse> result = categoryService.getAll();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(categoryResponse.getName(), result.get(0).getName());
            assertEquals(response2.getName(), result.get(1).getName());

            verify(categoryRepository).findAll();
            verify(categoryMapper, times(2)).toResponse(any(Category.class));
        }

        @Test
        @DisplayName("Should return empty list when no categories exist")
        void shouldReturnEmptyListWhenNoCategoriesExist() {
            // Given
            when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<CategoryResponse> result = categoryService.getAll();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(categoryRepository).findAll();
            verify(categoryMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should handle single category in list")
        void shouldHandleSingleCategoryInList() {
            // Given
            when(categoryRepository.findAll()).thenReturn(Collections.singletonList(category));
            when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

            // When
            List<CategoryResponse> result = categoryService.getAll();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(categoryResponse.getName(), result.get(0).getName());

            verify(categoryRepository).findAll();
            verify(categoryMapper).toResponse(category);
        }

        @Test
        @DisplayName("Should map all categories correctly")
        void shouldMapAllCategoriesCorrectly() {
            // Given
            Category cat1 = new Category();
            cat1.setCategoryId(UUID.randomUUID());
            cat1.setName("Category 1");

            Category cat2 = new Category();
            cat2.setCategoryId(UUID.randomUUID());
            cat2.setName("Category 2");

            Category cat3 = new Category();
            cat3.setCategoryId(UUID.randomUUID());
            cat3.setName("Category 3");

            List<Category> categories = Arrays.asList(cat1, cat2, cat3);

            CategoryResponse resp1 = new CategoryResponse();
            resp1.setCategoryId(cat1.getCategoryId());
            resp1.setName("Category 1");

            CategoryResponse resp2 = new CategoryResponse();
            resp2.setCategoryId(cat2.getCategoryId());
            resp2.setName("Category 2");

            CategoryResponse resp3 = new CategoryResponse();
            resp3.setCategoryId(cat3.getCategoryId());
            resp3.setName("Category 3");

            when(categoryRepository.findAll()).thenReturn(categories);
            when(categoryMapper.toResponse(cat1)).thenReturn(resp1);
            when(categoryMapper.toResponse(cat2)).thenReturn(resp2);
            when(categoryMapper.toResponse(cat3)).thenReturn(resp3);

            // When
            List<CategoryResponse> result = categoryService.getAll();

            // Then
            assertEquals(3, result.size());
            assertEquals("Category 1", result.get(0).getName());
            assertEquals("Category 2", result.get(1).getName());
            assertEquals("Category 3", result.get(2).getName());
        }
    }

    @Nested
    @DisplayName("Slug Generation Edge Cases")
    class SlugGenerationEdgeCasesTests {

        @Test
        @DisplayName("Should handle null input in slug generation")
        void shouldHandleNullInputInSlugGeneration() {
            // Given
            categoryRequest.setName(null);
            when(categoryRepository.findByName(null)).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            // Slug should be null for null input
            assertNull(captor.getValue().getSlug());
        }

        @Test
        @DisplayName("Should handle mixed case with numbers")
        void shouldHandleMixedCaseWithNumbers() {
            // Given
            categoryRequest.setName("Top 10 Recipes 2024");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertTrue(slug.matches("[a-z0-9-]+"));
            assertTrue(slug.contains("10"));
            assertTrue(slug.contains("2024"));
        }

        @Test
        @DisplayName("Should collapse multiple hyphens")
        void shouldCollapseMultipleHyphens() {
            // Given
            categoryRequest.setName("Asian - Pacific - Fusion");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            assertTrue(slug.matches("[a-z0-9-]+"));
        }

        @Test
        @DisplayName("Should handle accented characters from multiple languages")
        void shouldHandleAccentedCharactersFromMultipleLanguages() {
            // Given
            categoryRequest.setName("Café Français Español");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            assertTrue(slug.matches("[a-z0-9-]+"));
            assertFalse(slug.contains("é"));
            assertFalse(slug.contains("ñ"));
        }

        @Test
        @DisplayName("Should handle only special characters")
        void shouldHandleOnlySpecialCharacters() {
            // Given
            categoryRequest.setName("!@#$%^&*()");
            when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
            when(categoryMapper.toEntity(any())).thenReturn(category);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(categoryMapper.toResponse(any())).thenReturn(categoryResponse);

            // When
            categoryService.create(categoryRequest);

            // Then
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            // Should be empty or just hyphens after removing all special chars
        }
    }
}