package com.backend.cookshare.admin_report.controller;

import com.backend.cookshare.admin_report.dto.search_response.*;
import com.backend.cookshare.admin_report.service.SearchStatisticsService;
import com.backend.cookshare.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchStatisticsControllerTest {

    @Mock
    private SearchStatisticsService searchStatisticsService;

    @InjectMocks
    private SearchStatisticsController controller;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        startDate = LocalDateTime.now().minusDays(30);
        endDate = LocalDateTime.now();
    }

    @Test
    void getSearchOverview_ShouldReturnResponse() {
        SearchOverviewDTO overviewDTO = SearchOverviewDTO.builder()
                .totalSearches(100L)
                .uniqueSearchQueries(50L)
                .successfulSearches(80L)
                .totalUsers(20L)
                .averageResultsPerSearch(BigDecimal.valueOf(5.5))
                .build();

        when(searchStatisticsService.getSearchOverview(startDate, endDate)).thenReturn(overviewDTO);

        ResponseEntity<ApiResponse<SearchOverviewDTO>> response = controller.getSearchOverview(startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(overviewDTO, response.getBody().getData());
        verify(searchStatisticsService).getSearchOverview(startDate, endDate);
    }

    @Test
    void getPopularKeywords_ShouldReturnResponse() {
        PopularKeywordsDTO keywordsDTO = new PopularKeywordsDTO();
        when(searchStatisticsService.getPopularKeywords(10, startDate, endDate)).thenReturn(keywordsDTO);

        ResponseEntity<ApiResponse<PopularKeywordsDTO>> response = controller.getPopularKeywords(10, startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(keywordsDTO, response.getBody().getData());
        verify(searchStatisticsService).getPopularKeywords(10, startDate, endDate);
    }

    @Test
    void getPopularIngredients_ShouldReturnResponse() {
        PopularIngredientsDTO ingredientsDTO = new PopularIngredientsDTO();
        when(searchStatisticsService.getPopularIngredients(5, startDate, endDate)).thenReturn(ingredientsDTO);

        ResponseEntity<ApiResponse<PopularIngredientsDTO>> response = controller.getPopularIngredients(5, startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(ingredientsDTO, response.getBody().getData());
        verify(searchStatisticsService).getPopularIngredients(5, startDate, endDate);
    }

    @Test
    void getPopularCategories_ShouldReturnResponse() {
        PopularCategoriesDTO categoriesDTO = new PopularCategoriesDTO();
        when(searchStatisticsService.getPopularCategories(startDate, endDate)).thenReturn(categoriesDTO);

        ResponseEntity<ApiResponse<PopularCategoriesDTO>> response = controller.getPopularCategories(startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(categoriesDTO, response.getBody().getData());
        verify(searchStatisticsService).getPopularCategories(startDate, endDate);
    }

    @Test
    void getSearchSuccessRate_ShouldReturnResponse() {
        SearchSuccessRateDTO successRateDTO = new SearchSuccessRateDTO();
        when(searchStatisticsService.getSearchSuccessRate(startDate, endDate)).thenReturn(successRateDTO);

        ResponseEntity<ApiResponse<SearchSuccessRateDTO>> response = controller.getSearchSuccessRate(startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(successRateDTO, response.getBody().getData());
        verify(searchStatisticsService).getSearchSuccessRate(startDate, endDate);
    }

    @Test
    void getZeroResultKeywords_ShouldReturnResponse() {
        ZeroResultKeywordsDTO zeroDTO = new ZeroResultKeywordsDTO();
        when(searchStatisticsService.getZeroResultKeywords(20, startDate, endDate)).thenReturn(zeroDTO);

        ResponseEntity<ApiResponse<ZeroResultKeywordsDTO>> response = controller.getZeroResultKeywords(20, startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(zeroDTO, response.getBody().getData());
        verify(searchStatisticsService).getZeroResultKeywords(20, startDate, endDate);
    }

    @Test
    void getSearchTrends_ShouldReturnResponse() {
        SearchTrendsDTO trendsDTO = new SearchTrendsDTO();
        when(searchStatisticsService.getSearchTrends(startDate, endDate, "DAY")).thenReturn(trendsDTO);

        ResponseEntity<ApiResponse<SearchTrendsDTO>> response = controller.getSearchTrends(startDate, endDate, "DAY");

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(trendsDTO, response.getBody().getData());
        verify(searchStatisticsService).getSearchTrends(startDate, endDate, "DAY");
    }
}
