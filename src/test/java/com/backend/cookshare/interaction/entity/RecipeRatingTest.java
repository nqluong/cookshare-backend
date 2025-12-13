package com.backend.cookshare.interaction.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RecipeRatingTest {

    @Test
    void testAllArgsConstructorAndGetters() {
        UUID ratingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();
        Integer rating = 5;
        String review = "Excellent!";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        RecipeRating rr = new RecipeRating(
                ratingId, userId, recipeId, rating, review, createdAt, updatedAt
        );

        assertEquals(ratingId, rr.getRatingId());
        assertEquals(userId, rr.getUserId());
        assertEquals(recipeId, rr.getRecipeId());
        assertEquals(rating, rr.getRating());
        assertEquals(review, rr.getReview());
        assertEquals(createdAt, rr.getCreatedAt());
        assertEquals(updatedAt, rr.getUpdatedAt());
    }

    @Test
    void testNoArgsConstructorAndSetters() {
        RecipeRating rr = new RecipeRating();

        UUID ratingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();
        Integer rating = 4;
        String review = "Good!";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        rr.setRatingId(ratingId);
        rr.setUserId(userId);
        rr.setRecipeId(recipeId);
        rr.setRating(rating);
        rr.setReview(review);
        rr.setCreatedAt(createdAt);
        rr.setUpdatedAt(updatedAt);

        assertEquals(ratingId, rr.getRatingId());
        assertEquals(userId, rr.getUserId());
        assertEquals(recipeId, rr.getRecipeId());
        assertEquals(rating, rr.getRating());
        assertEquals(review, rr.getReview());
        assertEquals(createdAt, rr.getCreatedAt());
        assertEquals(updatedAt, rr.getUpdatedAt());
    }

    @Test
    void testBuilder() {
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        RecipeRating rr = RecipeRating.builder()
                .userId(userId)
                .recipeId(recipeId)
                .rating(3)
                .review("Average")
                .build();

        assertEquals(userId, rr.getUserId());
        assertEquals(recipeId, rr.getRecipeId());
        assertEquals(3, rr.getRating());
        assertEquals("Average", rr.getReview());
    }

    @Test
    void testPrePersistSetsCreatedAtAndUpdatedAt() {
        RecipeRating rr = new RecipeRating();
        assertNull(rr.getCreatedAt());
        assertNull(rr.getUpdatedAt());

        rr.onCreate();

        assertNotNull(rr.getCreatedAt());
        assertNotNull(rr.getUpdatedAt());
        assertEquals(rr.getCreatedAt(), rr.getUpdatedAt());
    }

    @Test
    void testPreUpdateSetsUpdatedAtOnly() throws InterruptedException {
        RecipeRating rr = new RecipeRating();
        rr.onCreate();

        LocalDateTime oldUpdated = rr.getUpdatedAt();

        Thread.sleep(5); // đảm bảo updatedAt thay đổi

        rr.onUpdate();

        assertNotNull(rr.getUpdatedAt());
        assertTrue(rr.getUpdatedAt().isAfter(oldUpdated));
        assertEquals(rr.getCreatedAt().getSecond(), rr.getCreatedAt().getSecond());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID id = UUID.randomUUID();

        RecipeRating r1 = RecipeRating.builder().ratingId(id).build();
        RecipeRating r2 = RecipeRating.builder().ratingId(id).build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
