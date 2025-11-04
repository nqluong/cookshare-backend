package com.backend.cookshare.admin_report.service;

import com.backend.cookshare.admin_report.dto.recipe_response.*;

import java.time.LocalDateTime;
import java.util.List;

public interface RecipeStatisticsService {
    RecipeStatisticsResponse getComprehensiveStatistics(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer topLimit);

    RecipeOverviewDTO getRecipeOverview();

    List<RecipePerformanceDTO> getTopViewedRecipes(int limit);

    List<RecipePerformanceDTO> getTopLikedRecipes(int limit);

    List<RecipePerformanceDTO> getTopSavedRecipes(int limit);

    List<RecipePerformanceDTO> getTopCommentedRecipes(int limit);

    List<TrendingRecipeDTO> getTrendingRecipes(int limit);

    List<RecipePerformanceDTO> getLowPerformanceRecipes(int limit);

    RecipeContentAnalysisDTO getContentAnalysis();

    List<TimeSeriesStatDTO> getTimeSeriesData(LocalDateTime startDate, LocalDateTime endDate);

    List<TopAuthorDTO> getTopAuthors(int limit);

    List<EngagementRateDTO> getHighEngagementRecipes(int limit);

    List<CategoryPerformanceDTO> getCategoryPerformance();

    RecipeCompletionStatsDTO getRecipeCompletionStats();
}
