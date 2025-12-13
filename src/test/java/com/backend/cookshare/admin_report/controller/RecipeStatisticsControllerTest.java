package com.backend.cookshare.admin_report.controller;

import com.backend.cookshare.admin_report.dto.recipe_response.*;
import com.backend.cookshare.admin_report.service.RecipeStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeStatisticsControllerTest {

    @Mock
    private RecipeStatisticsService statisticsService;

    @InjectMocks
    private RecipeStatisticsController controller;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        startDate = LocalDateTime.now().minusDays(30);
        endDate = LocalDateTime.now();
    }

    @Test
    void getComprehensiveStatistics_ShouldReturnResponse() {
        RecipeStatisticsResponse statsResponse = new RecipeStatisticsResponse();
        when(statisticsService.getComprehensiveStatistics(startDate, endDate, 10)).thenReturn(statsResponse);

        ResponseEntity<RecipeStatisticsResponse> response = controller.getComprehensiveStatistics(startDate, endDate, 10);

        assertNotNull(response);
        assertEquals(statsResponse, response.getBody());
        verify(statisticsService).getComprehensiveStatistics(startDate, endDate, 10);
    }

    @Test
    void getRecipeOverview_ShouldReturnResponse() {
        RecipeOverviewDTO overviewDTO = new RecipeOverviewDTO();
        when(statisticsService.getRecipeOverview()).thenReturn(overviewDTO);

        ResponseEntity<RecipeOverviewDTO> response = controller.getRecipeOverview();

        assertNotNull(response);
        assertEquals(overviewDTO, response.getBody());
        verify(statisticsService).getRecipeOverview();
    }

    @Test
    void getTopViewedRecipes_ShouldReturnResponse() {
        List<RecipePerformanceDTO> list = Collections.singletonList(new RecipePerformanceDTO());
        when(statisticsService.getTopViewedRecipes(10)).thenReturn(list);

        ResponseEntity<List<RecipePerformanceDTO>> response = controller.getTopViewedRecipes(10);

        assertNotNull(response);
        assertEquals(list, response.getBody());
        verify(statisticsService).getTopViewedRecipes(10);
    }

    @Test
    void getTopLikedRecipes_ShouldReturnResponse() {
        List<RecipePerformanceDTO> list = Collections.singletonList(new RecipePerformanceDTO());
        when(statisticsService.getTopLikedRecipes(10)).thenReturn(list);

        ResponseEntity<List<RecipePerformanceDTO>> response = controller.getTopLikedRecipes(10);

        assertNotNull(response);
        assertEquals(list, response.getBody());
        verify(statisticsService).getTopLikedRecipes(10);
    }

    @Test
    void getTopSavedRecipes_ShouldReturnResponse() {
        List<RecipePerformanceDTO> list = Collections.singletonList(new RecipePerformanceDTO());
        when(statisticsService.getTopSavedRecipes(10)).thenReturn(list);

        ResponseEntity<List<RecipePerformanceDTO>> response = controller.getTopSavedRecipes(10);

        assertNotNull(response);
        assertEquals(list, response.getBody());
        verify(statisticsService).getTopSavedRecipes(10);
    }

    @Test
    void getTopCommentedRecipes_ShouldReturnResponse() {
        List<RecipePerformanceDTO> list = Collections.singletonList(new RecipePerformanceDTO());
        when(statisticsService.getTopCommentedRecipes(10)).thenReturn(list);

        ResponseEntity<List<RecipePerformanceDTO>> response = controller.getTopCommentedRecipes(10);

        assertNotNull(response);
        assertEquals(list, response.getBody());
        verify(statisticsService).getTopCommentedRecipes(10);
    }

    @Test
    void getTrendingRecipes_ShouldReturnResponse() {
        List<TrendingRecipeDTO> list = Collections.singletonList(new TrendingRecipeDTO());
        when(statisticsService.getTrendingRecipes(10)).thenReturn(list);

        ResponseEntity<List<TrendingRecipeDTO>> response = controller.getTrendingRecipes(10);

        assertNotNull(response);
        assertEquals(list, response.getBody());
        verify(statisticsService).getTrendingRecipes(10);
    }

    @Test
    void getLowPerformanceRecipes_ShouldReturnResponse() {
        List<RecipePerformanceDTO> list = Collections.singletonList(new RecipePerformanceDTO());
        when(statisticsService.getLowPerformanceRecipes(10)).thenReturn(list);

        ResponseEntity<List<RecipePerformanceDTO>> response = controller.getLowPerformanceRecipes(10);

        assertNotNull(response);
        assertEquals(list, response.getBody());
        verify(statisticsService).getLowPerformanceRecipes(10);
    }

    @Test
    void getContentAnalysis_ShouldReturnResponse() {
        RecipeContentAnalysisDTO analysisDTO = new RecipeContentAnalysisDTO();
        when(statisticsService.getContentAnalysis()).thenReturn(analysisDTO);

        ResponseEntity<RecipeContentAnalysisDTO> response = controller.getContentAnalysis();

        assertNotNull(response);
        assertEquals(analysisDTO, response.getBody());
        verify(statisticsService).getContentAnalysis();
    }

    @Test
    void getTimeSeriesData_ShouldReturnResponse() {
        List<TimeSeriesStatDTO> list = Collections.singletonList(new TimeSeriesStatDTO());
        when(statisticsService.getTimeSeriesData(startDate, endDate)).thenReturn(list);

        ResponseEntity<List<TimeSeriesStatDTO>> response = controller.getTimeSeriesData(startDate, endDate);

        assertNotNull(response);
        assertEquals(list, response.getBody());
        verify(statisticsService).getTimeSeriesData(startDate, endDate);
    }

    @Test
    void getTopAuthors_ShouldReturnResponse() {
        List<TopAuthorDTO> list = Collections.singletonList(new TopAuthorDTO());
        when(statisticsService.getTopAuthors(10)).thenReturn(list);

        ResponseEntity<List<TopAuthorDTO>> response = controller.getTopAuthors(10);

        assertNotNull(response);
        assertEquals(list, response.getBody());
        verify(statisticsService).getTopAuthors(10);
    }

    @Test
    void getHighEngagementRecipes_ShouldReturnResponse() {
        List<EngagementRateDTO> list = Collections.singletonList(new EngagementRateDTO());
        when(statisticsService.getHighEngagementRecipes(10)).thenReturn(list);

        ResponseEntity<List<EngagementRateDTO>> response = controller.getHighEngagementRecipes(10);

        assertNotNull(response);
        assertEquals(list, response.getBody());
        verify(statisticsService).getHighEngagementRecipes(10);
    }

    @Test
    void getCategoryPerformance_ShouldReturnResponse() {
        List<CategoryPerformanceDTO> list = Collections.singletonList(new CategoryPerformanceDTO());
        when(statisticsService.getCategoryPerformance()).thenReturn(list);

        ResponseEntity<List<CategoryPerformanceDTO>> response = controller.getCategoryPerformance();

        assertNotNull(response);
        assertEquals(list, response.getBody());
        verify(statisticsService).getCategoryPerformance();
    }

    @Test
    void getRecipeCompletionStats_ShouldReturnResponse() {
        RecipeCompletionStatsDTO statsDTO = new RecipeCompletionStatsDTO();
        when(statisticsService.getRecipeCompletionStats()).thenReturn(statsDTO);

        ResponseEntity<RecipeCompletionStatsDTO> response = controller.getRecipeCompletionStats();

        assertNotNull(response);
        assertEquals(statsDTO, response.getBody());
        verify(statisticsService).getRecipeCompletionStats();
    }
}
