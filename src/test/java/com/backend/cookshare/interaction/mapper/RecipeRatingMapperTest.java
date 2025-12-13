package com.backend.cookshare.interaction.mapper;

import com.backend.cookshare.interaction.dto.response.RecipeRatingResponse;
import com.backend.cookshare.interaction.entity.RecipeRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecipeRatingMapperTest {

    private RecipeRatingMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(RecipeRatingMapper.class);
    }

    @Test
    void testToRecipeRatingResponse() {
        UUID ratingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        RecipeRating rating = RecipeRating.builder()
                .ratingId(ratingId)
                .userId(userId)
                .recipeId(recipeId)
                .rating(5)
                .review("Good")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        RecipeRatingResponse response = mapper.toRecipeRatingResponse(rating);

        assertNotNull(response);
        assertEquals(ratingId, response.getRatingId());
        assertEquals(userId, response.getUserId());
        assertEquals(recipeId, response.getRecipeId());
        assertEquals(5, response.getRating());
        assertEquals("Good", response.getReview());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void testNullInput() {
        assertNull(mapper.toRecipeRatingResponse(null));
    }
}
