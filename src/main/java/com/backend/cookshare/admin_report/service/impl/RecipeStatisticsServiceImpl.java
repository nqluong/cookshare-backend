package com.backend.cookshare.admin_report.service.impl;

import com.backend.cookshare.admin_report.dto.recipe_response.*;
import com.backend.cookshare.admin_report.repository.*;
import com.backend.cookshare.admin_report.repository.recipe_projection.*;
import com.backend.cookshare.admin_report.service.RecipeStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class RecipeStatisticsServiceImpl implements RecipeStatisticsService {
    private final RecipeStatisticsRepository statisticsRepository;
    private final Executor executorService;

    public RecipeStatisticsServiceImpl(RecipeStatisticsRepository statisticsRepository,
                                       @Qualifier("statisticsExecutor") Executor executorService) {
        this.statisticsRepository = statisticsRepository;
        this.executorService = executorService;
    }

    @Override
    public RecipeStatisticsResponse getComprehensiveStatistics(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer topLimit) {

        if (topLimit == null) topLimit = 10;
        final int finalLimit = topLimit;

        try {
            // Chạy song song tất cả các truy vấn độc lập
            CompletableFuture<RecipeOverviewDTO> overviewFuture =
                    CompletableFuture.supplyAsync(this::getRecipeOverview, executorService);

            CompletableFuture<List<RecipePerformanceDTO>> topViewedFuture =
                    CompletableFuture.supplyAsync(() -> getTopViewedRecipes(finalLimit), executorService);

            CompletableFuture<List<RecipePerformanceDTO>> topLikedFuture =
                    CompletableFuture.supplyAsync(() -> getTopLikedRecipes(finalLimit), executorService);

            CompletableFuture<List<RecipePerformanceDTO>> topSavedFuture =
                    CompletableFuture.supplyAsync(() -> getTopSavedRecipes(finalLimit), executorService);

            CompletableFuture<List<TrendingRecipeDTO>> trendingFuture =
                    CompletableFuture.supplyAsync(() -> getTrendingRecipes(finalLimit), executorService);

            CompletableFuture<List<RecipePerformanceDTO>> lowPerformanceFuture =
                    CompletableFuture.supplyAsync(() -> getLowPerformanceRecipes(finalLimit), executorService);

            CompletableFuture<RecipeContentAnalysisDTO> contentAnalysisFuture =
                    CompletableFuture.supplyAsync(this::getContentAnalysis, executorService);

            CompletableFuture<List<TimeSeriesStatDTO>> timeSeriesFuture =
                    CompletableFuture.supplyAsync(() -> getTimeSeriesData(startDate, endDate), executorService);

            // Đợi tất cả các future hoàn thành và tổng hợp kết quả
            CompletableFuture.allOf(
                    overviewFuture, topViewedFuture, topLikedFuture, topSavedFuture,
                    trendingFuture, lowPerformanceFuture, contentAnalysisFuture, timeSeriesFuture
            ).join();

            return RecipeStatisticsResponse.builder()
                    .overview(overviewFuture.get())
                    .topViewedRecipes(topViewedFuture.get())
                    .topLikedRecipes(topLikedFuture.get())
                    .topSavedRecipes(topSavedFuture.get())
                    .trendingRecipes(trendingFuture.get())
                    .lowPerformanceRecipes(lowPerformanceFuture.get())
                    .contentAnalysis(contentAnalysisFuture.get())
                    .timeSeriesData(timeSeriesFuture.get())
                    .build();
        } catch (Exception e) {
            log.error("Error fetching comprehensive statistics", e);
            throw new RuntimeException("Failed to fetch statistics", e);
        }
    }

    @Override
    public RecipeOverviewDTO getRecipeOverview() {
        log.debug("Fetching recipe overview");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.minusWeeks(1);
        LocalDateTime startOfMonth = now.minusMonths(1);
        LocalDateTime previousDay = startOfToday.minusDays(1);
        LocalDateTime previousWeek = startOfWeek.minusWeeks(1);
        LocalDateTime previousMonth = startOfMonth.minusMonths(1);

        try {
            // Chạy song song các truy vấn đếm
            CompletableFuture<Long> totalFuture =
                    CompletableFuture.supplyAsync(statisticsRepository::countTotalRecipes, executorService);

            CompletableFuture<Long> todayFuture =
                    CompletableFuture.supplyAsync(() -> statisticsRepository.countNewRecipes(startOfToday), executorService);

            CompletableFuture<Long> weekFuture =
                    CompletableFuture.supplyAsync(() -> statisticsRepository.countNewRecipes(startOfWeek), executorService);

            CompletableFuture<Long> monthFuture =
                    CompletableFuture.supplyAsync(() -> statisticsRepository.countNewRecipes(startOfMonth), executorService);

            CompletableFuture<Long> prevDayFuture =
                    CompletableFuture.supplyAsync(() -> statisticsRepository.countNewRecipes(previousDay), executorService);

            CompletableFuture<Long> prevWeekFuture =
                    CompletableFuture.supplyAsync(() -> statisticsRepository.countNewRecipes(previousWeek), executorService);

            CompletableFuture<Long> prevMonthFuture =
                    CompletableFuture.supplyAsync(() -> statisticsRepository.countNewRecipes(previousMonth), executorService);

            CompletableFuture<Map<String, Long>> byCategoryFuture =
                    CompletableFuture.supplyAsync(() ->
                            statisticsRepository.countRecipesByCategory()
                                    .stream()
                                    .collect(Collectors.toMap(
                                            CategoryDistribution::getName,
                                            CategoryDistribution::getCount,
                                            (a, b) -> a,
                                            LinkedHashMap::new
                                    )), executorService);

            CompletableFuture<Map<String, Long>> byDifficultyFuture =
                    CompletableFuture.supplyAsync(() ->
                            statisticsRepository.countRecipesByDifficulty()
                                    .stream()
                                    .collect(Collectors.toMap(
                                            DifficultyDistribution::getDifficulty,
                                            DifficultyDistribution::getCount,
                                            (a, b) -> a,
                                            LinkedHashMap::new
                                    )), executorService);

            // Đợi tất cả hoàn thành
            CompletableFuture.allOf(
                    totalFuture, todayFuture, weekFuture, monthFuture,
                    prevDayFuture, prevWeekFuture, prevMonthFuture,
                    byCategoryFuture, byDifficultyFuture
            ).join();

            // Lấy kết quả
            Long totalRecipes = totalFuture.get();
            Long newToday = todayFuture.get();
            Long newThisWeek = weekFuture.get();
            Long newThisMonth = monthFuture.get();
            Long previousDayCount = prevDayFuture.get() - newToday;
            Long previousWeekCount = prevWeekFuture.get() - newThisWeek;
            Long previousMonthCount = prevMonthFuture.get() - newThisMonth;

            // Tính tốc độ tăng trưởng
            Double dailyGrowth = calculateGrowthRate(newToday, previousDayCount);
            Double weeklyGrowth = calculateGrowthRate(newThisWeek, previousWeekCount);
            Double monthlyGrowth = calculateGrowthRate(newThisMonth, previousMonthCount);

            return RecipeOverviewDTO.builder()
                    .totalRecipes(totalRecipes)
                    .newRecipesToday(newToday)
                    .newRecipesThisWeek(newThisWeek)
                    .newRecipesThisMonth(newThisMonth)
                    .growthRateDaily(dailyGrowth)
                    .growthRateWeekly(weeklyGrowth)
                    .growthRateMonthly(monthlyGrowth)
                    .recipesByCategory(byCategoryFuture.get())
                    .recipesByDifficulty(byDifficultyFuture.get())
                    .build();
        } catch (Exception e) {
            log.error("Error fetching recipe overview", e);
            throw new RuntimeException("Failed to fetch recipe overview", e);
        }
    }


    @Override
    public List<RecipePerformanceDTO> getTopViewedRecipes(int limit) {
        log.debug("Fetching top {} viewed recipes", limit);

        return statisticsRepository.findTopViewedRecipes(limit)
                .stream()
                .map(this::mapToPerformanceDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecipePerformanceDTO> getTopLikedRecipes(int limit) {
        log.debug("Fetching top {} liked recipes", limit);

        return statisticsRepository.findTopLikedRecipes(limit)
                .stream()
                .map(this::mapToPerformanceDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecipePerformanceDTO> getTopSavedRecipes(int limit) {
        log.debug("Fetching top {} saved recipes", limit);

        return statisticsRepository.findTopSavedRecipes(limit)
                .stream()
                .map(this::mapToPerformanceDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecipePerformanceDTO> getTopCommentedRecipes(int limit) {
        log.debug("Fetching top {} commented recipes", limit);

        return statisticsRepository.findTopCommentedRecipes(limit)
                .stream()
                .map(projection -> {
                    RecipePerformanceDTO dto = mapToPerformanceDTO(projection);
                    dto.setCommentCount(projection.getCommentCount());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TrendingRecipeDTO> getTrendingRecipes(int limit) {
        log.debug("Fetching top {} trending recipes", limit);
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);

        return statisticsRepository.findTrendingRecipes(last7Days, limit)
                .stream()
                .map(projection -> TrendingRecipeDTO.builder()
                        .recipeId(projection.getRecipeId())
                        .title(projection.getTitle())
                        .slug(projection.getSlug())
                        .viewCount(projection.getViewCount())
                        .likeCount(projection.getLikeCount())
                        .trendingScore(projection.getTrendingScore())
                        .createdAt(projection.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<RecipePerformanceDTO> getLowPerformanceRecipes(int limit) {
        log.debug("Fetching top {} low performance recipes", limit);

        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        return statisticsRepository.findLowPerformanceRecipes(threshold, limit)
                .stream()
                .map(this::mapToPerformanceDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RecipeContentAnalysisDTO getContentAnalysis() {
        log.debug("Fetching content analysis");

        try {
            // Chạy song song các truy vấn phân tích
            CompletableFuture<CookingTimeStats> cookingTimesFuture =
                    CompletableFuture.supplyAsync(statisticsRepository::getAverageCookingTimes, executorService);

            CompletableFuture<Double> avgIngredientsFuture =
                    CompletableFuture.supplyAsync(statisticsRepository::getAverageIngredientCount, executorService);

            CompletableFuture<Double> avgStepsFuture =
                    CompletableFuture.supplyAsync(statisticsRepository::getAverageStepCount, executorService);

            CompletableFuture<MediaStats> mediaStatsFuture =
                    CompletableFuture.supplyAsync(statisticsRepository::getMediaStatistics, executorService);

            CompletableFuture<ContentLengthStats> contentLengthFuture =
                    CompletableFuture.supplyAsync(statisticsRepository::getAverageContentLength, executorService);

            // Đợi tất cả hoàn thành
            CompletableFuture.allOf(
                    cookingTimesFuture, avgIngredientsFuture, avgStepsFuture,
                    mediaStatsFuture, contentLengthFuture
            ).join();

            // Lấy kết quả
            CookingTimeStats cookingTimes = cookingTimesFuture.get();
            Double avgIngredients = avgIngredientsFuture.get();
            Double avgSteps = avgStepsFuture.get();
            MediaStats mediaStats = mediaStatsFuture.get();
            ContentLengthStats contentLength = contentLengthFuture.get();

            // Tính phần trăm media
            Long total = mediaStats.getTotalRecipes();
            Double imagePercentage = total > 0
                    ? (mediaStats.getRecipesWithImage() * 100.0 / total)
                    : 0.0;
            Double videoPercentage = total > 0
                    ? (mediaStats.getRecipesWithVideo() * 100.0 / total)
                    : 0.0;

            return RecipeContentAnalysisDTO.builder()
                    .avgCookTime(cookingTimes.getAvgCookTime())
                    .avgPrepTime(cookingTimes.getAvgPrepTime())
                    .avgTotalTime(cookingTimes.getAvgTotalTime())
                    .avgIngredientCount(avgIngredients != null ? avgIngredients : 0.0)
                    .avgStepCount(avgSteps != null ? avgSteps : 0.0)
                    .recipesWithImage(mediaStats.getRecipesWithImage())
                    .recipesWithVideo(mediaStats.getRecipesWithVideo())
                    .imagePercentage(imagePercentage)
                    .videoPercentage(videoPercentage)
                    .avgDescriptionLength(contentLength.getAvgDescriptionLength())
                    .avgInstructionLength(contentLength.getAvgInstructionLength())
                    .build();
        } catch (Exception e) {
            log.error("Error fetching content analysis", e);
            throw new RuntimeException("Failed to fetch content analysis", e);
        }
    }

    @Override
    public List<TimeSeriesStatDTO> getTimeSeriesData(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.now().minusMonths(1);
        if (endDate == null) endDate = LocalDateTime.now();

        log.debug("Fetching time series data from {} to {}", startDate, endDate);

        return statisticsRepository.getTimeSeriesData(startDate, endDate)
                .stream()
                .map(projection -> TimeSeriesStatDTO.builder()
                        .period(projection.getDate().toString())
                        .recipeCount(projection.getRecipeCount())
                        .viewCount(projection.getTotalViews())
                        .likeCount(projection.getTotalLikes())
                        .timestamp(projection.getDate().toLocalDate().atStartOfDay())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TopAuthorDTO> getTopAuthors(int limit) {
        log.debug("Fetching top {} authors", limit);

        return statisticsRepository.findTopAuthors(limit)
                .stream()
                .map(projection -> TopAuthorDTO.builder()
                        .userId(projection.getUserId())
                        .authorName(projection.getAuthorName())
                        .username(projection.getUsername())
                        .recipeCount(projection.getRecipeCount())
                        .totalViews(projection.getTotalViews())
                        .totalLikes(projection.getTotalLikes())
                        .avgRating(projection.getAvgRating())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<EngagementRateDTO> getHighEngagementRecipes(int limit) {
        log.debug("Fetching top {} high engagement recipes", limit);

        return statisticsRepository.findHighEngagementRecipes(limit)
                .stream()
                .map(projection -> EngagementRateDTO.builder()
                        .recipeId(projection.getRecipeId())
                        .title(projection.getTitle())
                        .viewCount(projection.getViewCount())
                        .engagementCount(projection.getEngagementCount())
                        .engagementRate(projection.getEngagementRate())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryPerformanceDTO> getCategoryPerformance() {
        log.debug("Fetching category performance");

        return statisticsRepository.getCategoryPerformance()
                .stream()
                .map(projection -> CategoryPerformanceDTO.builder()
                        .categoryName(projection.getCategoryName())
                        .recipeCount(projection.getRecipeCount())
                        .totalViews(projection.getTotalViews())
                        .totalLikes(projection.getTotalLikes())
                        .avgRating(projection.getAvgRating())
                        .avgEngagementRate(projection.getAvgEngagementRate())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public RecipeCompletionStatsDTO getRecipeCompletionStats() {
        log.debug("Fetching recipe completion statistics");

        RecipeCompletionStats stats = statisticsRepository.getRecipeCompletionStats();

        return RecipeCompletionStatsDTO.builder()
                .totalRecipes(stats.getTotalRecipes())
                .withDescription(stats.getWithDescription())
                .withImage(stats.getWithImage())
                .withVideo(stats.getWithVideo())
                .withIngredients(stats.getWithIngredients())
                .withSteps(stats.getWithSteps())
                .completeRecipes(stats.getCompleteRecipes())
                .completionRate(stats.getCompletionRate())
                .build();
    }

    // Helper methods
    private RecipePerformanceDTO mapToPerformanceDTO(RecipeProjection projection) {
        return RecipePerformanceDTO.builder()
                .recipeId(projection.getRecipeId())
                .title(projection.getTitle())
                .slug(projection.getSlug())
                .viewCount(projection.getViewCount())
                .likeCount(projection.getLikeCount())
                .saveCount(projection.getSaveCount())
                .averageRating(projection.getAverageRating())
                .ratingCount(projection.getRatingCount())
                .authorName(projection.getAuthorName())
                .createdAt(projection.getCreatedAt())
                .build();
    }

    private Double calculateGrowthRate(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return current != null && current > 0 ? 100.0 : 0.0;
        }
        if (current == null) current = 0L;
        return ((current - previous) * 100.0) / previous;
    }

}
