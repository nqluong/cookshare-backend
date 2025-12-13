package com.backend.cookshare.interaction.mapper;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.interaction.dto.response.RecipeLikeResponse;
import com.backend.cookshare.interaction.dto.response.RecipeSummaryResponse;
import com.backend.cookshare.interaction.entity.RecipeLike;
import com.backend.cookshare.recipe_management.entity.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import com.backend.cookshare.recipe_management.enums.Difficulty;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RecipeLikeMapperTest {

    private RecipeLikeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(RecipeLikeMapper.class);
    }

    @Test
    void testToRecipeLikeResponse() {
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        RecipeLike like = RecipeLike.builder()
                .userId(userId)
                .recipeId(recipeId)
                .createdAt(LocalDateTime.now())
                .build();

        RecipeLikeResponse response = mapper.toRecipeLikeResponse(like);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(recipeId, response.getRecipeId());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void testToRecipeResponse_WithSummary() {
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        RecipeLike like = RecipeLike.builder()
                .userId(userId)
                .recipeId(recipeId)
                .createdAt(LocalDateTime.now())
                .build();

        RecipeSummaryResponse summary = RecipeSummaryResponse.builder()
                .recipeId(recipeId)
                .title("Test Recipe")
                .slug("test-recipe")
                .description("A simple description")
                .build();

        RecipeLikeResponse response = mapper.toRecipeResponse(like, summary);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(recipeId, response.getRecipeId());
        assertEquals(summary, response.getRecipe());
    }

    @Test
    void testToRecipeSummary() {
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        User user = User.builder()
                .userId(userId)
                .username("tester")
                .fullName("Test User")
                .build();

        Recipe recipe = Recipe.builder()
                .recipeId(recipeId)
                .title("Demo Recipe")
                .slug("demo-recipe")
                .description("Delicious food")
                .featuredImage("img.png")
                .prepTime(10)
                .cookTime(20)
                .servings(2)
                .difficulty(Difficulty.EASY)
                .viewCount(100)
                .saveCount(5)
                .likeCount(12)
                .averageRating(BigDecimal.valueOf(4.5))
                .ratingCount(30)
                .isFeatured(true)
                .isPublished(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .build();

        RecipeSummaryResponse response = mapper.toRecipeSummary(recipe);

        assertNotNull(response);
        assertEquals(recipeId, response.getRecipeId());
        assertEquals("Demo Recipe", response.getTitle());
        assertEquals("tester", response.getUserName());
        assertEquals("Test User", response.getFullName());
        assertEquals("img.png", response.getFeaturedImage());
        assertEquals(10, response.getPrepTime());
        assertEquals(20, response.getCookTime());
    }
}
