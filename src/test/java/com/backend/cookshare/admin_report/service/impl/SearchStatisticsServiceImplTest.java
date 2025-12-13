package com.backend.cookshare.admin_report.service.impl;

import com.backend.cookshare.admin_report.dto.search_response.*;
import com.backend.cookshare.admin_report.repository.SearchStatisticsRepository;
import com.backend.cookshare.admin_report.repository.search_projection.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchStatisticsServiceImplTest {

    @Mock
    private SearchStatisticsRepository searchRepository;

    @InjectMocks
    private SearchStatisticsServiceImpl searchService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.now().minusDays(30);
        endDate = LocalDateTime.now();
    }

    // ==================== getSearchOverview ====================
    @Test
    void getSearchOverview_ShouldReturnCorrectOverview() {
        when(searchRepository.countTotalSearches(startDate, endDate)).thenReturn(100L);
        when(searchRepository.countUniqueQueries(startDate, endDate)).thenReturn(50L);
        when(searchRepository.countSuccessfulSearches(startDate, endDate)).thenReturn(80L);
        when(searchRepository.countUniqueSearchUsers(startDate, endDate)).thenReturn(20L);
        when(searchRepository.getAverageResultsPerSearch(startDate, endDate)).thenReturn(BigDecimal.valueOf(5.5));

        SearchOverviewDTO result = searchService.getSearchOverview(startDate, endDate);

        assertEquals(100L, result.getTotalSearches());
        assertEquals(50L, result.getUniqueSearchQueries());
        assertEquals(80L, result.getSuccessfulSearches());
        assertEquals(20L, result.getTotalUsers());
        assertEquals(BigDecimal.valueOf(5.5), result.getAverageResultsPerSearch());

        verify(searchRepository).countTotalSearches(startDate, endDate);
    }

    @Test
    void getSearchOverview_ShouldHandleZeroUsers() {
        when(searchRepository.countTotalSearches(startDate, endDate)).thenReturn(10L);
        when(searchRepository.countUniqueQueries(startDate, endDate)).thenReturn(5L);
        when(searchRepository.countSuccessfulSearches(startDate, endDate)).thenReturn(8L);
        when(searchRepository.countUniqueSearchUsers(startDate, endDate)).thenReturn(0L);
        when(searchRepository.getAverageResultsPerSearch(startDate, endDate)).thenReturn(BigDecimal.valueOf(1.0));

        SearchOverviewDTO result = searchService.getSearchOverview(startDate, endDate);

        assertEquals(BigDecimal.ZERO, result.getAverageSearchesPerUser());
    }

    // ==================== getPopularKeywords ====================
    @Test
    void getPopularKeywords_ShouldReturnList() {
        PopularKeywordProjection projection = mock(PopularKeywordProjection.class);
        when(projection.getSearchQuery()).thenReturn("chicken");
        when(projection.getSearchCount()).thenReturn(10L);
        when(projection.getUniqueUsers()).thenReturn(5L);
        when(projection.getAvgResults()).thenReturn(BigDecimal.valueOf(3));
        when(projection.getLastSearched()).thenReturn(LocalDateTime.now());

        List<PopularKeywordProjection> keywordList = Arrays.asList(projection);

        when(searchRepository.getPopularKeywords(5, startDate, endDate)).thenReturn(keywordList);
        when(searchRepository.countUniqueQueries(startDate, endDate)).thenReturn(50L);

        PopularKeywordsDTO result = searchService.getPopularKeywords(5, startDate, endDate);

        assertEquals(1, result.getKeywords().size());
        assertEquals("chicken", result.getKeywords().get(0).getKeyword());
    }

    // ==================== getPopularIngredients ====================
    @Test
    void getPopularIngredients_ShouldReturnList() {
        PopularIngredientProjection projection = mock(PopularIngredientProjection.class);
        when(projection.getIngredientId()).thenReturn(UUID.randomUUID());
        when(projection.getIngredientName()).thenReturn("Tomato");
        when(projection.getSearchCount()).thenReturn(10L);
        when(projection.getDirectSearches()).thenReturn(3L);
        when(projection.getRecipeCount()).thenReturn(5L);

        when(searchRepository.getPopularIngredients(5, startDate, endDate))
                .thenReturn(Collections.singletonList(projection));

        PopularIngredientsDTO result = searchService.getPopularIngredients(5, startDate, endDate);

        assertEquals(1, result.getIngredients().size());
        assertEquals("Tomato", result.getIngredients().get(0).getIngredientName());
        assertEquals(BigDecimal.valueOf(2.0).setScale(2), result.getIngredients().get(0).getSearchToRecipeRatio());
    }

    // ==================== getPopularCategories ====================
    @Test
    void getPopularCategories_ShouldReturnList() {
        CategoryViewProjection projection = mock(CategoryViewProjection.class);
        when(projection.getCategoryId()).thenReturn(UUID.randomUUID());
        when(projection.getCategoryName()).thenReturn("Dessert");
        when(projection.getViewCount()).thenReturn(50L);
        when(projection.getRecipeCount()).thenReturn(10L);
        when(projection.getUniqueUsers()).thenReturn(20L);

        when(searchRepository.getCategoryViewStats(startDate, endDate))
                .thenReturn(Collections.singletonList(projection));

        PopularCategoriesDTO result = searchService.getPopularCategories(startDate, endDate);

        assertEquals(1, result.getCategories().size());
        assertEquals("Dessert", result.getCategories().get(0).getCategoryName());
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), result.getCategories().get(0).getViewShare().setScale(2));
    }

    // ==================== getSearchSuccessRate ====================
    @Test
    void getSearchSuccessRate_ShouldReturnCorrectRates() {
        when(searchRepository.countTotalSearches(startDate, endDate)).thenReturn(10L);
        when(searchRepository.countSuccessfulSearches(startDate, endDate)).thenReturn(8L);

        SuccessRateByTypeProjection typeProjection = mock(SuccessRateByTypeProjection.class);
        when(typeProjection.getSearchType()).thenReturn("RECIPE");
        when(typeProjection.getTotalSearches()).thenReturn(5L);
        when(typeProjection.getSuccessfulSearches()).thenReturn(4L);

        when(searchRepository.getSuccessRateByType(startDate, endDate))
                .thenReturn(Collections.singletonList(typeProjection));

        SuccessRateTrendProjection trendProjection = mock(SuccessRateTrendProjection.class);
        when(trendProjection.getPeriod()).thenReturn(LocalDateTime.now());
        when(trendProjection.getTotalSearches()).thenReturn(5L);
        when(trendProjection.getSuccessfulSearches()).thenReturn(4L);

        when(searchRepository.getSuccessRateTrend(startDate, endDate, "DAY"))
                .thenReturn(Collections.singletonList(trendProjection));

        SearchSuccessRateDTO result = searchService.getSearchSuccessRate(startDate, endDate);

        assertEquals(10L, result.getTotalSearches());
        assertEquals(8L, result.getSuccessfulSearches());
        assertEquals(2L, result.getFailedSearches());
        assertEquals(1, result.getSuccessByType().size());
        assertEquals(1, result.getTrendData().size());
    }

    // ==================== getZeroResultKeywords ====================
    @Test
    void getZeroResultKeywords_ShouldReturnDTOWithSuggestedActions() {
        ZeroResultKeywordProjection projection = mock(ZeroResultKeywordProjection.class);
        when(projection.getSearchQuery()).thenReturn("ab1");
        when(projection.getSearchCount()).thenReturn(2L);
        when(projection.getUniqueUsers()).thenReturn(1L);
        when(projection.getFirstSearched()).thenReturn(LocalDateTime.now());
        when(projection.getLastSearched()).thenReturn(LocalDateTime.now());

        when(searchRepository.getZeroResultKeywords(5, startDate, endDate))
                .thenReturn(Collections.singletonList(projection));
        when(searchRepository.countTotalSearches(startDate, endDate)).thenReturn(100L);
        when(searchRepository.countZeroResultSearches(startDate, endDate)).thenReturn(2L);

        ZeroResultKeywordsDTO result = searchService.getZeroResultKeywords(5, startDate, endDate);

        assertEquals(1, result.getKeywords().size());
        assertTrue(result.getKeywords().get(0).getSuggestedActions().size() >= 1);
    }

    // ==================== getSearchTrends ====================
    @Test
    void getSearchTrends_ShouldReturnTrendsAndGrowth() {
        SearchTrendProjection projection1 = mock(SearchTrendProjection.class);
        when(projection1.getPeriod()).thenReturn(LocalDateTime.now().minusDays(1));
        when(projection1.getTotalSearches()).thenReturn(5L);
        when(projection1.getSuccessfulSearches()).thenReturn(3L);
        when(projection1.getUniqueUsers()).thenReturn(2L);
        when(projection1.getUniqueQueries()).thenReturn(1L);
        when(projection1.getAvgResults()).thenReturn(BigDecimal.valueOf(2));

        SearchTrendProjection projection2 = mock(SearchTrendProjection.class);
        when(projection2.getPeriod()).thenReturn(LocalDateTime.now());
        when(projection2.getTotalSearches()).thenReturn(10L);
        when(projection2.getSuccessfulSearches()).thenReturn(5L);
        when(projection2.getUniqueUsers()).thenReturn(3L);
        when(projection2.getUniqueQueries()).thenReturn(2L);
        when(projection2.getAvgResults()).thenReturn(BigDecimal.valueOf(4));

        when(searchRepository.getSearchTrends(startDate, endDate, "DAY"))
                .thenReturn(Arrays.asList(projection1, projection2));

        SearchTrendsDTO result = searchService.getSearchTrends(startDate, endDate, "DAY");

        assertEquals(2, result.getTrendData().size());
        assertTrue(result.getGrowthRate().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(result.getPeakPeriod());
    }
}
