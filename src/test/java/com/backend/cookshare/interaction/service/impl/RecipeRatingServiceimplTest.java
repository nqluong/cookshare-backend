package com.backend.cookshare.interaction.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.interaction.dto.response.RecipeRatingResponse;
import com.backend.cookshare.interaction.entity.RecipeRating;
import com.backend.cookshare.interaction.mapper.RecipeRatingMapper;
import com.backend.cookshare.interaction.repository.RecipeRatingRespository;
import com.backend.cookshare.interaction.sevice.impl.RecipeRatingServiceimpl;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RecipeRatingServiceimplTest {

    @InjectMocks
    RecipeRatingServiceimpl recipeRatingService;

    @Mock
    UserRepository userRepository;

    @Mock
    RecipeRepository recipeRepository;

    @Mock
    RecipeRatingRespository recipeRatingRespository;

    @Mock
    RecipeRatingMapper recipeRatingMapper;

    @Mock
    SecurityContext securityContext;

    @Mock
    Authentication authentication;

    private UUID userId;
    private UUID recipeId;
    private User user;
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        recipeId = UUID.randomUUID();

        user = User.builder()
                .userId(userId)
                .username("testuser")
                .build();

        recipe = Recipe.builder()
                .recipeId(recipeId)
                .ratingCount(1)
                .build();

        // Mock SecurityContext
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    }

    // -------------------------------------------------------------
    // Test: Create new rating
    // -------------------------------------------------------------
    @Test
    void testRatingRecipe_NewRating() {

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeRatingRespository.findByUserIdAndRecipeId(userId, recipeId)).thenReturn(Optional.empty());
        when(recipeRatingRespository.save(any())).thenAnswer(inv -> inv.getArguments()[0]);
        when(recipeRatingRespository.getAverageRatingByRecipeId(recipeId))
                .thenReturn(BigDecimal.valueOf(4.5));

        RecipeRatingResponse response = recipeRatingService.ratingrecipe(recipeId, 5);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals(BigDecimal.valueOf(4.5), response.getAverageRating());
        assertEquals(2, recipe.getRatingCount());

        verify(recipeRatingRespository).save(any());
        verify(recipeRepository).save(recipe);
    }

    // -------------------------------------------------------------
    // Test: Update existing rating
    // -------------------------------------------------------------
    @Test
    void testRatingRecipe_UpdateExisting() {
        RecipeRating existing = RecipeRating.builder()
                .ratingId(UUID.randomUUID())
                .userId(userId)
                .recipeId(recipeId)
                .rating(3)
                .build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeRatingRespository.findByUserIdAndRecipeId(userId, recipeId))
                .thenReturn(Optional.of(existing));
        when(recipeRatingRespository.save(any())).thenAnswer(inv -> inv.getArguments()[0]);
        when(recipeRatingRespository.getAverageRatingByRecipeId(recipeId))
                .thenReturn(BigDecimal.valueOf(4.0));

        RecipeRatingResponse response = recipeRatingService.ratingrecipe(recipeId, 4);

        assertNotNull(response);
        assertEquals(4, response.getRating());
        assertEquals(BigDecimal.valueOf(4.0), response.getAverageRating());

        // Rating count must NOT increase when updating
        assertEquals(1, recipe.getRatingCount());
    }

    // -------------------------------------------------------------
    // Test: Recipe not found
    // -------------------------------------------------------------
    @Test
    void testRatingRecipe_RecipeNotFound() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> recipeRatingService.ratingrecipe(recipeId, 5));

        assertEquals(ErrorCode.RECIPE_NOT_FOUND, ex.getErrorCode());
    }

    // -------------------------------------------------------------
    // Test: isRecipeRated()
    // -------------------------------------------------------------
    @Test
    void testIsRecipeRated() {
        when(recipeRatingRespository.existsByUserIdAndRecipeId(userId, recipeId))
                .thenReturn(true);

        assertTrue(recipeRatingService.isRecipeRated(recipeId));
    }

    // -------------------------------------------------------------
    // Test: getMyRating()
    // -------------------------------------------------------------
    @Test
    void testGetMyRating() {
        RecipeRating rating = RecipeRating.builder()
                .rating(4)
                .build();

        when(recipeRatingRespository.findByUserIdAndRecipeId(userId, recipeId))
                .thenReturn(Optional.of(rating));

        assertEquals(4, recipeRatingService.getMyRating(recipeId));
    }

    @Test
    void testGetMyRating_NotRated() {
        when(recipeRatingRespository.findByUserIdAndRecipeId(userId, recipeId))
                .thenReturn(Optional.empty());

        assertNull(recipeRatingService.getMyRating(recipeId));
    }
}
