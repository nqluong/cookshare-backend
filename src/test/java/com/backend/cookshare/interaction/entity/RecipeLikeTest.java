package com.backend.cookshare.interaction.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RecipeLikeTest {

    @Test
    void testAllArgsConstructorAndGetters() {
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        RecipeLike like = new RecipeLike(userId, recipeId, createdAt);

        assertEquals(userId, like.getUserId());
        assertEquals(recipeId, like.getRecipeId());
        assertEquals(createdAt, like.getCreatedAt());
    }

    @Test
    void testNoArgsConstructorAndSetters() {
        RecipeLike like = new RecipeLike();

        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        like.setUserId(userId);
        like.setRecipeId(recipeId);
        like.setCreatedAt(createdAt);

        assertEquals(userId, like.getUserId());
        assertEquals(recipeId, like.getRecipeId());
        assertEquals(createdAt, like.getCreatedAt());
    }

    @Test
    void testBuilder() {
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        RecipeLike like = RecipeLike.builder()
                .userId(userId)
                .recipeId(recipeId)
                .createdAt(createdAt)
                .build();

        assertEquals(userId, like.getUserId());
        assertEquals(recipeId, like.getRecipeId());
        assertEquals(createdAt, like.getCreatedAt());
    }

    @Test
    void testPrePersistSetsCreatedAt() {
        RecipeLike like = new RecipeLike();
        assertNull(like.getCreatedAt());

        like.onCreate();

        assertNotNull(like.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        RecipeLike like1 = new RecipeLike(userId, recipeId, LocalDateTime.now());
        RecipeLike like2 = new RecipeLike(userId, recipeId, LocalDateTime.now());

        assertEquals(like1, like2);
        assertEquals(like1.hashCode(), like2.hashCode());
    }
}
