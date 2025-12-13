package com.backend.cookshare.interaction.dto.response;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SearchHistoryResponseTest {

    @Test
    void testBuilder_ShouldBuildCorrectly() {
        UUID searchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String query = "chicken soup";
        String type = "recipe";

        LocalDateTime now = LocalDateTime.now();

        SearchHistoryResponse response = SearchHistoryResponse.builder()
                .searchId(searchId)
                .userId(userId)
                .searchQuery(query)
                .searchType(type)
                .resultCount(5)
                .createdAt(now)
                .build();

        assertThat(response.getSearchId()).isEqualTo(searchId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getSearchQuery()).isEqualTo(query);
        assertThat(response.getSearchType()).isEqualTo(type);
        assertThat(response.getResultCount()).isEqualTo(5);
        assertThat(response.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void testNoArgsConstructor_ShouldHaveDefaultValues() {
        SearchHistoryResponse response = new SearchHistoryResponse();

        assertThat(response.getSearchId()).isNull();
        assertThat(response.getUserId()).isNull();
        assertThat(response.getSearchQuery()).isNull();
        assertThat(response.getSearchType()).isNull();

        // Default @Builder.Default không áp dụng cho constructor => phải test đúng là 0
        assertThat(response.getResultCount()).isEqualTo(0);

        assertThat(response.getCreatedAt()).isNull();
    }

    @Test
    void testAllArgsConstructor_ShouldAssignValues() {
        UUID searchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String query = "pizza";
        String type = "food";
        int resultCount = 10;
        LocalDateTime createdAt = LocalDateTime.now();

        SearchHistoryResponse response = new SearchHistoryResponse(
                searchId,
                userId,
                query,
                type,
                resultCount,
                createdAt
        );

        assertThat(response.getSearchId()).isEqualTo(searchId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getSearchQuery()).isEqualTo(query);
        assertThat(response.getSearchType()).isEqualTo(type);
        assertThat(response.getResultCount()).isEqualTo(resultCount);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void testOnCreate_ShouldSetCreatedAt() {
        SearchHistoryResponse response = new SearchHistoryResponse();

        assertThat(response.getCreatedAt()).isNull();

        response.onCreate();

        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
