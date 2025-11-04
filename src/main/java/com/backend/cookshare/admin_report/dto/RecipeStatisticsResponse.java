package com.backend.cookshare.admin_report.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeStatisticsResponse {
    RecipeOverviewDTO overview;
    List<RecipePerformanceDTO> topViewedRecipes;
    List<RecipePerformanceDTO> topLikedRecipes;
    List<RecipePerformanceDTO> topSavedRecipes;
    List<TrendingRecipeDTO> trendingRecipes;
    List<RecipePerformanceDTO> lowPerformanceRecipes;
    RecipeContentAnalysisDTO contentAnalysis;
    List<TimeSeriesStatDTO> timeSeriesData;
}