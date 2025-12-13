package com.backend.cookshare.interaction.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SearchHistoryTest {

    @Test
    void testAllArgsConstructorAndGetters() {
        UUID searchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String searchQuery = "recipe chicken";
        String searchType = "RECIPE";
        Integer resultCount = 12;
        LocalDateTime createdAt = LocalDateTime.now();

        SearchHistory history = new SearchHistory(
                searchId,
                userId,
                searchQuery,
                searchType,
                resultCount,
                createdAt
        );

        assertEquals(searchId, history.getSearchId());
        assertEquals(userId, history.getUserId());
        assertEquals(searchQuery, history.getSearchQuery());
        assertEquals(searchType, history.getSearchType());
        assertEquals(resultCount, history.getResultCount());
        assertEquals(createdAt, history.getCreatedAt());
    }

    @Test
    void testNoArgsConstructorAndSetters() {
        SearchHistory history = new SearchHistory();

        UUID searchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String searchQuery = "cake";
        String searchType = "BAKING";
        Integer resultCount = 5;
        LocalDateTime createdAt = LocalDateTime.now();

        history.setSearchId(searchId);
        history.setUserId(userId);
        history.setSearchQuery(searchQuery);
        history.setSearchType(searchType);
        history.setResultCount(resultCount);
        history.setCreatedAt(createdAt);

        assertEquals(searchId, history.getSearchId());
        assertEquals(userId, history.getUserId());
        assertEquals(searchQuery, history.getSearchQuery());
        assertEquals(searchType, history.getSearchType());
        assertEquals(resultCount, history.getResultCount());
        assertEquals(createdAt, history.getCreatedAt());
    }

    @Test
    void testBuilder() {
        UUID userId = UUID.randomUUID();

        SearchHistory history = SearchHistory.builder()
                .userId(userId)
                .searchQuery("pasta")
                .searchType("FOOD")
                .build();

        assertEquals(userId, history.getUserId());
        assertEquals("pasta", history.getSearchQuery());
        assertEquals("FOOD", history.getSearchType());
        assertEquals(0, history.getResultCount()); // @Builder.Default
    }

    @Test
    void testPrePersistSetsCreatedAt() {
        SearchHistory history = new SearchHistory();
        assertNull(history.getCreatedAt());

        history.onCreate();

        assertNotNull(history.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID id = UUID.randomUUID();

        SearchHistory h1 = SearchHistory.builder()
                .searchId(id)
                .build();

        SearchHistory h2 = SearchHistory.builder()
                .searchId(id)
                .build();

        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
    }
}
