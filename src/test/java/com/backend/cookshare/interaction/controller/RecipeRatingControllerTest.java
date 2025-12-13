package com.backend.cookshare.interaction.controller;

import com.backend.cookshare.common.dto.ApiResponse;
import com.backend.cookshare.interaction.dto.response.RecipeRatingResponse;
import com.backend.cookshare.interaction.sevice.RecipeRatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeRatingControllerTest {

    @Mock
    private RecipeRatingService recipeRatingService;

    @InjectMocks
    private RecipeRatingController recipeRatingController;

    private UUID recipeId;
    private RecipeRatingResponse ratingResponse;

    @BeforeEach
    void setUp() {
        recipeId = UUID.randomUUID();

        ratingResponse = RecipeRatingResponse.builder()
                .recipeId(recipeId)
                .rating(4)
                .build();
    }

    @Test
    void rateRecipe_ShouldReturnRatingResult() {
        // Arrange
        int rating = 4;
        when(recipeRatingService.ratingrecipe(recipeId, rating)).thenReturn(ratingResponse);

        // Act
        ApiResponse<RecipeRatingResponse> response =
                recipeRatingController.rateRecipe(recipeId, rating);

        // Assert
        assertNotNull(response);
        assertEquals(4, response.getData().getRating());
        assertEquals(recipeId, response.getData().getRecipeId());

        verify(recipeRatingService).ratingrecipe(recipeId, rating);
    }

    @Test
    void isRecipeRated_ShouldReturnBoolean() {
        when(recipeRatingService.isRecipeRated(recipeId)).thenReturn(true);

        com.backend.cookshare.recipe_management.dto.ApiResponse<Boolean> response =
                recipeRatingController.isRecipeLiked(recipeId);

        assertNotNull(response);
        assertTrue(response.getResult());
        verify(recipeRatingService).isRecipeRated(recipeId);
    }

    @Test
    void getMyRating_ShouldReturnUserRating() {
        when(recipeRatingService.getMyRating(recipeId)).thenReturn(5);

        ApiResponse<Integer> response = recipeRatingController.getMyRating(recipeId);

        assertNotNull(response);
        assertEquals(5, response.getData());
        assertEquals("Đã tìm thấy đánh giá của bạn", response.getMessage());

        verify(recipeRatingService).getMyRating(recipeId);
    }

    @Test
    void getMyRating_ShouldReturnNullIfNoRating() {
        when(recipeRatingService.getMyRating(recipeId)).thenReturn(null);

        ApiResponse<Integer> response = recipeRatingController.getMyRating(recipeId);

        assertNotNull(response);
        assertNull(response.getData());
        assertEquals("Chưa đánh giá", response.getMessage());

        verify(recipeRatingService).getMyRating(recipeId);
    }
}
