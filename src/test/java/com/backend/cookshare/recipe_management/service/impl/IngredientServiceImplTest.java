package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.request.IngredientRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import com.backend.cookshare.recipe_management.mapper.IngredientMapper;
import com.backend.cookshare.recipe_management.repository.IngredientRepository;
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
class IngredientServiceImplTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private IngredientMapper ingredientMapper;

    @InjectMocks
    private IngredientServiceImpl ingredientService;

    private UUID testId;
    private IngredientRequest ingredientRequest;
    private Ingredient ingredient;
    private RecipeIngredientResponse ingredientResponse;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        ingredientRequest = new IngredientRequest();
        ingredientRequest.setName("Tomato");

        ingredient = new Ingredient();
        ingredient.setIngredientId(testId);
        ingredient.setName("Tomato");
        ingredient.setSlug("tomato");
        ingredient.setUsageCount(0);
        ingredient.setCreatedAt(LocalDateTime.now());

        ingredientResponse = new RecipeIngredientResponse();
        ingredientResponse.setIngredientId(testId);
        ingredientResponse.setName("Tomato");
        ingredientResponse.setSlug("tomato");
    }

    @Nested
    @DisplayName("Create Ingredient Tests")
    class CreateIngredientTests {

        @Test
        @DisplayName("Should create ingredient successfully")
        void shouldCreateIngredientSuccessfully() {
            // Given
            when(ingredientRepository.findByNameIgnoreCase(ingredientRequest.getName()))
                    .thenReturn(Optional.empty());
            when(ingredientMapper.toEntity(ingredientRequest)).thenReturn(ingredient);
            when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);
            when(ingredientMapper.toResponse(ingredient)).thenReturn(ingredientResponse);

            // When
            RecipeIngredientResponse result = ingredientService.createIngredient(ingredientRequest);

            // Then
            assertNotNull(result);
            assertEquals(ingredientResponse.getIngredientId(), result.getIngredientId());
            assertEquals(ingredientResponse.getName(), result.getName());

            ArgumentCaptor<Ingredient> ingredientCaptor = ArgumentCaptor.forClass(Ingredient.class);
            verify(ingredientRepository).save(ingredientCaptor.capture());

            Ingredient savedIngredient = ingredientCaptor.getValue();
            assertNotNull(savedIngredient.getSlug());
            assertNotNull(savedIngredient.getCreatedAt());
            assertEquals(0, savedIngredient.getUsageCount());

            verify(ingredientRepository).findByNameIgnoreCase(ingredientRequest.getName());
            verify(ingredientMapper).toEntity(ingredientRequest);
            verify(ingredientMapper).toResponse(ingredient);
        }

        @Test
        @DisplayName("Should throw exception when ingredient already exists")
        void shouldThrowExceptionWhenIngredientAlreadyExists() {
            // Given
            when(ingredientRepository.findByNameIgnoreCase(ingredientRequest.getName()))
                    .thenReturn(Optional.of(ingredient));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> ingredientService.createIngredient(ingredientRequest));

            assertEquals(ErrorCode.TAG_ALREADY_EXISTS, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Nguyên liệu đã tồn tại"));

            verify(ingredientRepository).findByNameIgnoreCase(ingredientRequest.getName());
            verify(ingredientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should generate correct slug for ingredient name")
        void shouldGenerateCorrectSlug() {
            // Given
            ingredientRequest.setName("Chicken Breast 123");
            when(ingredientRepository.findByNameIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(ingredientMapper.toEntity(any())).thenReturn(ingredient);
            when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);
            when(ingredientMapper.toResponse(any())).thenReturn(ingredientResponse);

            // When
            ingredientService.createIngredient(ingredientRequest);

            // Then
            ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
            verify(ingredientRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertNotNull(slug);
            assertTrue(slug.matches("[a-z0-9-]+"));
            assertFalse(slug.startsWith("-"));
            assertFalse(slug.endsWith("-"));
        }

        @Test
        @DisplayName("Should handle special characters in ingredient name")
        void shouldHandleSpecialCharactersInName() {
            // Given
            ingredientRequest.setName("Chili Pepper! @#$");
            when(ingredientRepository.findByNameIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(ingredientMapper.toEntity(any())).thenReturn(ingredient);
            when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);
            when(ingredientMapper.toResponse(any())).thenReturn(ingredientResponse);

            // When
            ingredientService.createIngredient(ingredientRequest);

            // Then
            ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
            verify(ingredientRepository).save(captor.capture());
            assertNotNull(captor.getValue().getSlug());
        }
    }

    @Nested
    @DisplayName("Get Ingredient Tests")
    class GetIngredientTests {

        @Test
        @DisplayName("Should get ingredient by id successfully")
        void shouldGetIngredientByIdSuccessfully() {
            // Given
            when(ingredientRepository.findById(testId)).thenReturn(Optional.of(ingredient));
            when(ingredientMapper.toResponse(ingredient)).thenReturn(ingredientResponse);

            // When
            RecipeIngredientResponse result = ingredientService.getIngredientById(testId);

            // Then
            assertNotNull(result);
            assertEquals(ingredientResponse.getIngredientId(), result.getIngredientId());
            assertEquals(ingredientResponse.getName(), result.getName());

            verify(ingredientRepository).findById(testId);
            verify(ingredientMapper).toResponse(ingredient);
        }

        @Test
        @DisplayName("Should throw exception when ingredient not found by id")
        void shouldThrowExceptionWhenIngredientNotFoundById() {
            // Given
            when(ingredientRepository.findById(testId)).thenReturn(Optional.empty());

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> ingredientService.getIngredientById(testId));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Không tìm thấy nguyên liệu"));

            verify(ingredientRepository).findById(testId);
            verify(ingredientMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("Update Ingredient Tests")
    class UpdateIngredientTests {

        @Test
        @DisplayName("Should update ingredient successfully")
        void shouldUpdateIngredientSuccessfully() {
            // Given
            IngredientRequest updateRequest = new IngredientRequest();
            updateRequest.setName("Updated Tomato");

            when(ingredientRepository.findById(testId)).thenReturn(Optional.of(ingredient));
            doNothing().when(ingredientMapper).updateIngredientFromDto(updateRequest, ingredient);
            when(ingredientRepository.save(ingredient)).thenReturn(ingredient);
            when(ingredientMapper.toResponse(ingredient)).thenReturn(ingredientResponse);

            // When
            RecipeIngredientResponse result = ingredientService.updateIngredient(testId, updateRequest);

            // Then
            assertNotNull(result);
            assertEquals(ingredientResponse.getIngredientId(), result.getIngredientId());

            verify(ingredientRepository).findById(testId);
            verify(ingredientMapper).updateIngredientFromDto(updateRequest, ingredient);
            verify(ingredientRepository).save(ingredient);
            verify(ingredientMapper).toResponse(ingredient);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent ingredient")
        void shouldThrowExceptionWhenUpdatingNonExistentIngredient() {
            // Given
            IngredientRequest updateRequest = new IngredientRequest();
            updateRequest.setName("Updated Name");

            when(ingredientRepository.findById(testId)).thenReturn(Optional.empty());

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> ingredientService.updateIngredient(testId, updateRequest));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Không tìm thấy nguyên liệu"));

            verify(ingredientRepository).findById(testId);
            verify(ingredientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should regenerate slug when updating ingredient name")
        void shouldRegenerateSlugWhenUpdatingName() {
            // Given
            IngredientRequest updateRequest = new IngredientRequest();
            updateRequest.setName("New Ingredient Name");

            when(ingredientRepository.findById(testId)).thenReturn(Optional.of(ingredient));
            doAnswer(invocation -> {
                ingredient.setName("New Ingredient Name");
                return null;
            }).when(ingredientMapper).updateIngredientFromDto(updateRequest, ingredient);
            when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);
            when(ingredientMapper.toResponse(any())).thenReturn(ingredientResponse);

            // When
            ingredientService.updateIngredient(testId, updateRequest);

            // Then
            ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
            verify(ingredientRepository).save(captor.capture());
            assertNotNull(captor.getValue().getSlug());
        }
    }

    @Nested
    @DisplayName("Delete Ingredient Tests")
    class DeleteIngredientTests {

        @Test
        @DisplayName("Should delete ingredient successfully")
        void shouldDeleteIngredientSuccessfully() {
            // Given
            when(ingredientRepository.existsById(testId)).thenReturn(true);
            doNothing().when(ingredientRepository).deleteById(testId);

            // When
            ingredientService.deleteIngredient(testId);

            // Then
            verify(ingredientRepository).existsById(testId);
            verify(ingredientRepository).deleteById(testId);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent ingredient")
        void shouldThrowExceptionWhenDeletingNonExistentIngredient() {
            // Given
            when(ingredientRepository.existsById(testId)).thenReturn(false);

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> ingredientService.deleteIngredient(testId));

            assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Không tìm thấy nguyên liệu để xóa"));

            verify(ingredientRepository).existsById(testId);
            verify(ingredientRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Get All Ingredients Tests")
    class GetAllIngredientsTests {

        @Test
        @DisplayName("Should get all ingredients successfully")
        void shouldGetAllIngredientsSuccessfully() {
            // Given
            Ingredient ingredient2 = new Ingredient();
            ingredient2.setIngredientId(UUID.randomUUID());
            ingredient2.setName("Onion");

            RecipeIngredientResponse response2 = new RecipeIngredientResponse();
            response2.setIngredientId(ingredient2.getIngredientId());
            response2.setName("Onion");

            List<Ingredient> ingredients = Arrays.asList(ingredient, ingredient2);

            when(ingredientRepository.findAll()).thenReturn(ingredients);
            when(ingredientMapper.toResponse(ingredient)).thenReturn(ingredientResponse);
            when(ingredientMapper.toResponse(ingredient2)).thenReturn(response2);

            // When
            List<RecipeIngredientResponse> result = ingredientService.getAllIngredients();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(ingredientResponse.getName(), result.get(0).getName());
            assertEquals(response2.getName(), result.get(1).getName());

            verify(ingredientRepository).findAll();
            verify(ingredientMapper, times(2)).toResponse(any(Ingredient.class));
        }

        @Test
        @DisplayName("Should return empty list when no ingredients exist")
        void shouldReturnEmptyListWhenNoIngredientsExist() {
            // Given
            when(ingredientRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<RecipeIngredientResponse> result = ingredientService.getAllIngredients();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(ingredientRepository).findAll();
            verify(ingredientMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should handle single ingredient in list")
        void shouldHandleSingleIngredientInList() {
            // Given
            when(ingredientRepository.findAll()).thenReturn(Collections.singletonList(ingredient));
            when(ingredientMapper.toResponse(ingredient)).thenReturn(ingredientResponse);

            // When
            List<RecipeIngredientResponse> result = ingredientService.getAllIngredients();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(ingredientResponse.getName(), result.get(0).getName());

            verify(ingredientRepository).findAll();
            verify(ingredientMapper).toResponse(ingredient);
        }
    }

    @Nested
    @DisplayName("Slug Generation Tests")
    class SlugGenerationTests {

        @Test
        @DisplayName("Should handle uppercase letters in slug generation")
        void shouldHandleUppercaseLettersInSlugGeneration() {
            // Given
            ingredientRequest.setName("UPPERCASE INGREDIENT");
            when(ingredientRepository.findByNameIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(ingredientMapper.toEntity(any())).thenReturn(ingredient);
            when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);
            when(ingredientMapper.toResponse(any())).thenReturn(ingredientResponse);

            // When
            ingredientService.createIngredient(ingredientRequest);

            // Then
            ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
            verify(ingredientRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertTrue(slug.matches("[a-z0-9-]+"));
        }

        @Test
        @DisplayName("Should handle multiple spaces in slug generation")
        void shouldHandleMultipleSpacesInSlugGeneration() {
            // Given
            ingredientRequest.setName("Ingredient   With   Spaces");
            when(ingredientRepository.findByNameIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(ingredientMapper.toEntity(any())).thenReturn(ingredient);
            when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);
            when(ingredientMapper.toResponse(any())).thenReturn(ingredientResponse);

            // When
            ingredientService.createIngredient(ingredientRequest);

            // Then
            ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
            verify(ingredientRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertFalse(slug.contains("  "));
            assertFalse(slug.startsWith("-"));
            assertFalse(slug.endsWith("-"));
        }

        @Test
        @DisplayName("Should handle leading and trailing special characters")
        void shouldHandleLeadingAndTrailingSpecialCharacters() {
            // Given
            ingredientRequest.setName("!!!Ingredient@@@");
            when(ingredientRepository.findByNameIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());
            when(ingredientMapper.toEntity(any())).thenReturn(ingredient);
            when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);
            when(ingredientMapper.toResponse(any())).thenReturn(ingredientResponse);

            // When
            ingredientService.createIngredient(ingredientRequest);

            // Then
            ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
            verify(ingredientRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertFalse(slug.startsWith("-"));
            assertFalse(slug.endsWith("-"));
        }
    }
}