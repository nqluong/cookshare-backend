package com.backend.cookshare.interaction.mapper;

import com.backend.cookshare.interaction.dto.response.SearchHistoryResponse;
import com.backend.cookshare.interaction.entity.SearchHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SearchHistoryMapperTest {

    private SearchHistoryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(SearchHistoryMapper.class);
    }

    @Test
    void testToSearchHistoryResponse() {
        UUID searchId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        SearchHistory history = SearchHistory.builder()
                .searchId(searchId)
                .userId(userId)
                .searchQuery("chicken soup")
                .searchType("RECIPE")
                .resultCount(12)
                .createdAt(LocalDateTime.now())
                .build();

        SearchHistoryResponse response = mapper.toSearchHistoryResponse(history);

        assertNotNull(response);
        assertEquals(searchId, response.getSearchId());
        assertEquals(userId, response.getUserId());
        assertEquals("chicken soup", response.getSearchQuery());
        assertEquals("RECIPE", response.getSearchType());
        assertEquals(12, response.getResultCount());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void testNullInput() {
        assertNull(mapper.toSearchHistoryResponse(null));
    }
}
