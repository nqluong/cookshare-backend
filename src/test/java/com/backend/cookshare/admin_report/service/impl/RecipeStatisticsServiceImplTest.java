package com.backend.cookshare.admin_report.service.impl;

import com.backend.cookshare.admin_report.dto.recipe_response.*;
import com.backend.cookshare.admin_report.repository.*;
import com.backend.cookshare.admin_report.repository.recipe_projection.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeStatisticsServiceImplTest {

    @Mock
    private RecipeStatisticsRepository statisticsRepository;

    private Executor directExecutor = Runnable::run;

    @InjectMocks
    private RecipeStatisticsServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new RecipeStatisticsServiceImpl(statisticsRepository, directExecutor);
    }

    // ------------------------ Recipe Overview ------------------------
    @Test
    void testGetRecipeOverview() {
        when(statisticsRepository.countTotalRecipes()).thenReturn(100L);
        when(statisticsRepository.countNewRecipes(any())).thenReturn(10L);

        CategoryDistribution cat = mock(CategoryDistribution.class);
        when(cat.getName()).thenReturn("Dessert");
        when(cat.getCount()).thenReturn(30L);
        when(statisticsRepository.countRecipesByCategory()).thenReturn(List.of(cat));

        DifficultyDistribution diff = mock(DifficultyDistribution.class);
        when(diff.getDifficulty()).thenReturn("EASY");
        when(diff.getCount()).thenReturn(20L);
        when(statisticsRepository.countRecipesByDifficulty()).thenReturn(List.of(diff));

        RecipeOverviewDTO dto = service.getRecipeOverview();

        assertNotNull(dto);
        assertEquals(100L, dto.getTotalRecipes());
        assertEquals(10L, dto.getNewRecipesToday());
        assertTrue(dto.getRecipesByCategory().containsKey("Dessert"));
    }

    @Test
    void testGetRecipeOverviewEmpty() {
        when(statisticsRepository.countTotalRecipes()).thenReturn(0L);
        when(statisticsRepository.countNewRecipes(any())).thenReturn(0L);
        when(statisticsRepository.countRecipesByCategory()).thenReturn(Collections.emptyList());
        when(statisticsRepository.countRecipesByDifficulty()).thenReturn(Collections.emptyList());

        RecipeOverviewDTO dto = service.getRecipeOverview();

        assertEquals(0L, dto.getTotalRecipes());
        assertTrue(dto.getRecipesByCategory().isEmpty());
    }

    // ------------------------ Top Viewed ------------------------
    @Test
    void testGetTopViewedRecipes() {
        RecipeProjection p = mock(RecipeProjection.class);
        when(p.getRecipeId()).thenReturn("1");
        when(p.getTitle()).thenReturn("Cake");
        when(p.getSlug()).thenReturn("cake");
        when(p.getViewCount()).thenReturn(200L);
        when(p.getLikeCount()).thenReturn(40L);
        when(p.getSaveCount()).thenReturn(5L);
        when(p.getAverageRating()).thenReturn(BigDecimal.valueOf(4.5));
        when(p.getRatingCount()).thenReturn(10L);
        when(p.getAuthorName()).thenReturn("Alice");
        when(p.getCreatedAt()).thenReturn(LocalDateTime.now());

        when(statisticsRepository.findTopViewedRecipes(10)).thenReturn(List.of(p));

        var list = service.getTopViewedRecipes(10);

        assertEquals(1, list.size());
        assertEquals("Cake", list.get(0).getTitle());
    }

    @Test
    void testGetTopViewedRecipesEmpty() {
        when(statisticsRepository.findTopViewedRecipes(10)).thenReturn(Collections.emptyList());
        var list = service.getTopViewedRecipes(10);
        assertTrue(list.isEmpty());
    }

    @Test
    void testTopViewedRecipesNull() {
        when(statisticsRepository.findTopViewedRecipes(10)).thenReturn(Collections.emptyList());
        var list = service.getTopViewedRecipes(10);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------ Top Liked ------------------------
    @Test
    void testGetTopLikedRecipes() {
        RecipeProjection p = mock(RecipeProjection.class);
        when(p.getTitle()).thenReturn("Pho");
        when(statisticsRepository.findTopLikedRecipes(5)).thenReturn(List.of(p));
        var list = service.getTopLikedRecipes(5);
        assertEquals("Pho", list.get(0).getTitle());
    }

    @Test
    void testTopLikedRecipesEmpty() {
        when(statisticsRepository.findTopLikedRecipes(5)).thenReturn(Collections.emptyList());
        var list = service.getTopLikedRecipes(5);
        assertTrue(list.isEmpty());
    }

    @Test
    void testTopLikedRecipesNull() {
        when(statisticsRepository.findTopLikedRecipes(5)).thenReturn(Collections.emptyList());
        var list = service.getTopLikedRecipes(5);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------ Top Saved ------------------------
    @Test
    void testGetTopSavedRecipes() {
        RecipeProjection p = mock(RecipeProjection.class);
        when(p.getTitle()).thenReturn("Spring Roll");
        when(statisticsRepository.findTopSavedRecipes(3)).thenReturn(List.of(p));
        var list = service.getTopSavedRecipes(3);
        assertEquals("Spring Roll", list.get(0).getTitle());
    }

    @Test
    void testTopSavedRecipesEmpty() {
        when(statisticsRepository.findTopSavedRecipes(3)).thenReturn(Collections.emptyList());
        var list = service.getTopSavedRecipes(3);
        assertTrue(list.isEmpty());
    }

    @Test
    void testTopSavedRecipesNull() {
        when(statisticsRepository.findTopSavedRecipes(3)).thenReturn(Collections.emptyList());
        var list = service.getTopSavedRecipes(3);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------ Top Commented ------------------------
    @Test
    void testGetTopCommentedRecipes() {
        RecipeWithCommentProjection p = mock(RecipeWithCommentProjection.class);
        when(p.getTitle()).thenReturn("Noodle");
        when(p.getCommentCount()).thenReturn(50L);
        when(statisticsRepository.findTopCommentedRecipes(5)).thenReturn(List.of(p));
        var list = service.getTopCommentedRecipes(5);
        assertEquals(50L, list.get(0).getCommentCount());
    }

    @Test
    void testTopCommentedRecipesEmpty() {
        when(statisticsRepository.findTopCommentedRecipes(5)).thenReturn(Collections.emptyList());
        var list = service.getTopCommentedRecipes(5);
        assertTrue(list.isEmpty());
    }

    @Test
    void testTopCommentedRecipesNull() {
        when(statisticsRepository.findTopCommentedRecipes(5)).thenReturn(Collections.emptyList());
        var list = service.getTopCommentedRecipes(5);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------ Trending ------------------------
    @Test
    void testGetTrendingRecipes() {
        TrendingRecipeProjection p = mock(TrendingRecipeProjection.class);
        when(p.getTitle()).thenReturn("Hot Soup");
        when(p.getTrendingScore()).thenReturn(99.9);
        when(statisticsRepository.findTrendingRecipes(any(), eq(5))).thenReturn(List.of(p));
        var list = service.getTrendingRecipes(5);
        assertEquals("Hot Soup", list.get(0).getTitle());
    }

    @Test
    void testTrendingRecipesEmpty() {
        when(statisticsRepository.findTrendingRecipes(any(), eq(5))).thenReturn(Collections.emptyList());
        var list = service.getTrendingRecipes(5);
        assertTrue(list.isEmpty());
    }

    @Test
    void testTrendingRecipesNull() {
        when(statisticsRepository.findTrendingRecipes(any(), eq(5))).thenReturn(Collections.emptyList());
        var list = service.getTrendingRecipes(5);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------ Low Performance ------------------------
    @Test
    void testLowPerformanceRecipes() {
        RecipeProjection p = mock(RecipeProjection.class);
        when(p.getTitle()).thenReturn("Weak Dish");
        when(statisticsRepository.findLowPerformanceRecipes(any(), eq(4))).thenReturn(List.of(p));
        var list = service.getLowPerformanceRecipes(4);
        assertEquals("Weak Dish", list.get(0).getTitle());
    }

    @Test
    void testLowPerformanceRecipesEmpty() {
        when(statisticsRepository.findLowPerformanceRecipes(any(), eq(4))).thenReturn(Collections.emptyList());
        var list = service.getLowPerformanceRecipes(4);
        assertTrue(list.isEmpty());
    }

    @Test
    void testLowPerformanceRecipesNull() {
        when(statisticsRepository.findLowPerformanceRecipes(any(), eq(4))).thenReturn(Collections.emptyList());
        var list = service.getLowPerformanceRecipes(4);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------ Content Analysis ------------------------
    @Test
    void testContentAnalysis() throws Exception {
        CookingTimeStats cook = mock(CookingTimeStats.class);
        when(cook.getAvgCookTime()).thenReturn(10.0);
        when(cook.getAvgPrepTime()).thenReturn(5.0);
        when(cook.getAvgTotalTime()).thenReturn(15.0);
        when(statisticsRepository.getAverageCookingTimes()).thenReturn(cook);
        when(statisticsRepository.getAverageIngredientCount()).thenReturn(7.0);
        when(statisticsRepository.getAverageStepCount()).thenReturn(5.0);

        MediaStats ms = mock(MediaStats.class);
        when(ms.getTotalRecipes()).thenReturn(100L);
        when(ms.getRecipesWithImage()).thenReturn(80L);
        when(ms.getRecipesWithVideo()).thenReturn(20L);
        when(statisticsRepository.getMediaStatistics()).thenReturn(ms);

        ContentLengthStats cl = mock(ContentLengthStats.class);
        when(cl.getAvgDescriptionLength()).thenReturn(100.0);
        when(cl.getAvgInstructionLength()).thenReturn(200.0);
        when(statisticsRepository.getAverageContentLength()).thenReturn(cl);

        RecipeContentAnalysisDTO dto = service.getContentAnalysis();
        assertEquals(80L, dto.getRecipesWithImage());
        assertEquals(20.0, dto.getVideoPercentage());
    }

    @Test
    void testContentAnalysisNullMedia() throws Exception {
        CookingTimeStats cookStats = mock(CookingTimeStats.class);
        when(cookStats.getAvgCookTime()).thenReturn(0.0);
        when(cookStats.getAvgPrepTime()).thenReturn(0.0);
        when(cookStats.getAvgTotalTime()).thenReturn(0.0);

        MediaStats mediaStats = mock(MediaStats.class);
        when(mediaStats.getTotalRecipes()).thenReturn(0L);
        when(mediaStats.getRecipesWithImage()).thenReturn(0L);
        when(mediaStats.getRecipesWithVideo()).thenReturn(0L);

        ContentLengthStats contentStats = mock(ContentLengthStats.class);
        when(contentStats.getAvgDescriptionLength()).thenReturn(0.0);
        when(contentStats.getAvgInstructionLength()).thenReturn(0.0);

        when(statisticsRepository.getAverageCookingTimes()).thenReturn(cookStats);
        when(statisticsRepository.getAverageIngredientCount()).thenReturn(0.0);
        when(statisticsRepository.getAverageStepCount()).thenReturn(0.0);
        when(statisticsRepository.getMediaStatistics()).thenReturn(mediaStats);
        when(statisticsRepository.getAverageContentLength()).thenReturn(contentStats);

        RecipeContentAnalysisDTO dto = service.getContentAnalysis();
        assertNotNull(dto);
        assertEquals(0.0, dto.getVideoPercentage());
        assertEquals(0L, dto.getRecipesWithImage());
    }

    // ------------------------ Time Series ------------------------
    @Test
    void testTimeSeriesData() {
        TimeSeriesProjection p = mock(TimeSeriesProjection.class);
        when(p.getDate()).thenReturn(java.sql.Date.valueOf("2024-01-10"));
        when(p.getRecipeCount()).thenReturn(10L);
        when(p.getTotalViews()).thenReturn(200L);
        when(p.getTotalLikes()).thenReturn(30L);
        when(statisticsRepository.getTimeSeriesData(any(), any())).thenReturn(List.of(p));

        var list = service.getTimeSeriesData(LocalDateTime.now().minusDays(7), LocalDateTime.now());
        assertEquals(1, list.size());
        assertEquals("2024-01-10", list.get(0).getPeriod());
    }

    @Test
    void testTimeSeriesDataNull() {
        when(statisticsRepository.getTimeSeriesData(any(), any())).thenReturn(Collections.emptyList());
        var list = service.getTimeSeriesData(LocalDateTime.now().minusDays(7), LocalDateTime.now());
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void testTimeSeriesDataEmpty() {
        when(statisticsRepository.getTimeSeriesData(any(), any())).thenReturn(Collections.emptyList());
        var list = service.getTimeSeriesData(LocalDateTime.now().minusDays(7), LocalDateTime.now());
        assertTrue(list.isEmpty());
    }

    // ------------------------ Top Authors ------------------------
    @Test
    void testTopAuthors() {
        TopAuthorProjection p = mock(TopAuthorProjection.class);
        when(p.getAuthorName()).thenReturn("Alice");
        when(p.getUsername()).thenReturn("alice123");
        when(p.getRecipeCount()).thenReturn(15L);
        when(statisticsRepository.findTopAuthors(10)).thenReturn(List.of(p));

        var list = service.getTopAuthors(10);
        assertEquals("Alice", list.get(0).getAuthorName());
    }

    @Test
    void testTopAuthorsNull() {
        when(statisticsRepository.findTopAuthors(10)).thenReturn(Collections.emptyList());
        var list = service.getTopAuthors(10);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------ High Engagement ------------------------
    @Test
    void testHighEngagementRecipes() {
        EngagementRateProjection p = mock(EngagementRateProjection.class);
        when(p.getTitle()).thenReturn("Pho Special");
        when(p.getEngagementRate()).thenReturn(99.0);
        when(statisticsRepository.findHighEngagementRecipes(3)).thenReturn(List.of(p));

        var list = service.getHighEngagementRecipes(3);
        assertEquals(99.0, list.get(0).getEngagementRate());
    }

    @Test
    void testHighEngagementRecipesNull() {
        when(statisticsRepository.findHighEngagementRecipes(3)).thenReturn(Collections.emptyList());
        var list = service.getHighEngagementRecipes(3);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------ Category Performance ------------------------
    @Test
    void testCategoryPerformance() {
        CategoryPerformanceProjection p = mock(CategoryPerformanceProjection.class);
        when(p.getCategoryName()).thenReturn("Dessert");
        when(p.getRecipeCount()).thenReturn(50L);
        when(statisticsRepository.getCategoryPerformance()).thenReturn(List.of(p));

        var list = service.getCategoryPerformance();
        assertEquals("Dessert", list.get(0).getCategoryName());
    }

    @Test
    void testCategoryPerformanceNull() {
        when(statisticsRepository.getCategoryPerformance()).thenReturn(Collections.emptyList());
        var list = service.getCategoryPerformance();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ------------------------ Recipe Completion ------------------------
    @Test
    void testRecipeCompletionStats() {
        RecipeCompletionStats p = mock(RecipeCompletionStats.class);
        when(p.getTotalRecipes()).thenReturn(100L);
        when(p.getCompleteRecipes()).thenReturn(70L);
        when(p.getCompletionRate()).thenReturn(70.0);
        when(statisticsRepository.getRecipeCompletionStats()).thenReturn(p);

        RecipeCompletionStatsDTO dto = service.getRecipeCompletionStats();
        assertEquals(70.0, dto.getCompletionRate());
    }

    @Test
    void testCompletionStatsNull() {
        RecipeCompletionStats stats = mock(RecipeCompletionStats.class);
        when(stats.getTotalRecipes()).thenReturn(0L);
        when(stats.getCompleteRecipes()).thenReturn(0L);
        when(stats.getCompletionRate()).thenReturn(0.0);

        when(statisticsRepository.getRecipeCompletionStats()).thenReturn(stats);

        var dto = service.getRecipeCompletionStats();
        assertNotNull(dto);
        assertEquals(0.0, dto.getCompletionRate());
    }
    @Test
    void testGetComprehensiveStatistics() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        // Mock các repository trả dữ liệu giả lập
        when(statisticsRepository.countTotalRecipes()).thenReturn(10L);
        when(statisticsRepository.countNewRecipes(any())).thenReturn(2L);
        when(statisticsRepository.countRecipesByCategory()).thenReturn(Collections.emptyList());
        when(statisticsRepository.countRecipesByDifficulty()).thenReturn(Collections.emptyList());
        when(statisticsRepository.findTopViewedRecipes(anyInt())).thenReturn(Collections.emptyList());
        when(statisticsRepository.findTopLikedRecipes(anyInt())).thenReturn(Collections.emptyList());
        when(statisticsRepository.findTopSavedRecipes(anyInt())).thenReturn(Collections.emptyList());
        when(statisticsRepository.findTrendingRecipes(any(), anyInt())).thenReturn(Collections.emptyList());
        when(statisticsRepository.findLowPerformanceRecipes(any(), anyInt())).thenReturn(Collections.emptyList());
        when(statisticsRepository.getAverageCookingTimes()).thenReturn(mock(CookingTimeStats.class));
        when(statisticsRepository.getAverageIngredientCount()).thenReturn(0.0);
        when(statisticsRepository.getAverageStepCount()).thenReturn(0.0);
        when(statisticsRepository.getMediaStatistics()).thenReturn(mock(MediaStats.class));
        when(statisticsRepository.getAverageContentLength()).thenReturn(mock(ContentLengthStats.class));
        when(statisticsRepository.getTimeSeriesData(any(), any())).thenReturn(Collections.emptyList());

        var stats = service.getComprehensiveStatistics(start, end, 5);

        assertNotNull(stats);
        assertNotNull(stats.getOverview());
        assertTrue(stats.getTopViewedRecipes().isEmpty());
    }

    // ------------------------ Async executor test ------------------------
    @Test
    void testAsyncExecutorRunsSynchronously() {
        assertDoesNotThrow(() -> service.getTopViewedRecipes(1));
    }
}