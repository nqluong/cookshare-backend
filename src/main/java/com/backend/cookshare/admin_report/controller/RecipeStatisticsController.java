package com.backend.cookshare.admin_report.controller;

import com.backend.cookshare.admin_report.dto.*;
import com.backend.cookshare.admin_report.service.RecipeStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/statistics/recipes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RecipeStatisticsController {

    private final RecipeStatisticsService statisticsService;

    /**
     * Lấy thống kê tổng hợp - bao gồm tất cả các metrics
     */
    @GetMapping("/comprehensive")
    public ResponseEntity<RecipeStatisticsResponse> getComprehensiveStatistics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        RecipeStatisticsResponse stats = statisticsService.getComprehensiveStatistics(
                startDate, endDate, limit);

        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy thống kê tổng quan
     */
    @GetMapping("/overview")
    public ResponseEntity<RecipeOverviewDTO> getRecipeOverview() {
        RecipeOverviewDTO overview = statisticsService.getRecipeOverview();

        return ResponseEntity.ok(overview);
    }

    /**
     * Top công thức có lượt xem cao nhất
     */
    @GetMapping("/top-viewed")
    public ResponseEntity<List<RecipePerformanceDTO>> getTopViewedRecipes(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        List<RecipePerformanceDTO> recipes = statisticsService.getTopViewedRecipes(limit);

        return ResponseEntity.ok(recipes);
    }

    /**
     * Top công thức được like nhiều nhất
     */
    @GetMapping("/top-liked")
    public ResponseEntity<List<RecipePerformanceDTO>> getTopLikedRecipes(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        List<RecipePerformanceDTO> recipes = statisticsService.getTopLikedRecipes(limit);

        return ResponseEntity.ok(recipes);
    }

    /**
     * Top công thức được lưu nhiều nhất
     */
    @GetMapping("/top-saved")
    public ResponseEntity<List<RecipePerformanceDTO>> getTopSavedRecipes(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        List<RecipePerformanceDTO> recipes = statisticsService.getTopSavedRecipes(limit);

        return ResponseEntity.ok(recipes);
    }

    /**
     * Top công thức có nhiều comment nhất
     */
    @GetMapping("/top-commented")
    public ResponseEntity<List<RecipePerformanceDTO>> getTopCommentedRecipes(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        List<RecipePerformanceDTO> recipes = statisticsService.getTopCommentedRecipes(limit);

        return ResponseEntity.ok(recipes);
    }

    /**
     * Top công thức đang trending
     */
    @GetMapping("/trending")
    public ResponseEntity<List<TrendingRecipeDTO>> getTrendingRecipes(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        List<TrendingRecipeDTO> recipes = statisticsService.getTrendingRecipes(limit);

        return ResponseEntity.ok(recipes);
    }

    /**
     * Công thức có hiệu suất thấp
     */
    @GetMapping("/low-performance")
    public ResponseEntity<List<RecipePerformanceDTO>> getLowPerformanceRecipes(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        List<RecipePerformanceDTO> recipes = statisticsService.getLowPerformanceRecipes(limit);

        return ResponseEntity.ok(recipes);
    }

    /**
     * Phân tích nội dung công thức
     */
    @GetMapping("/content-analysis")
    public ResponseEntity<RecipeContentAnalysisDTO> getContentAnalysis() {

        RecipeContentAnalysisDTO analysis = statisticsService.getContentAnalysis();

        return ResponseEntity.ok(analysis);
    }

    /**
     * Dữ liệu time series theo khoảng thời gian
     */
    @GetMapping("/time-series")
    public ResponseEntity<List<TimeSeriesStatDTO>> getTimeSeriesData(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<TimeSeriesStatDTO> data = statisticsService.getTimeSeriesData(startDate, endDate);

        return ResponseEntity.ok(data);
    }

    /**
     * Top tác giả có nhiều công thức nhất
     */
    @GetMapping("/top-authors")
    public ResponseEntity<List<TopAuthorDTO>> getTopAuthors(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        List<TopAuthorDTO> authors = statisticsService.getTopAuthors(limit);

        return ResponseEntity.ok(authors);
    }

    /**
     * Công thức có engagement rate cao
     */
    @GetMapping("/high-engagement")
    public ResponseEntity<List<EngagementRateDTO>> getHighEngagementRecipes(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        List<EngagementRateDTO> recipes = statisticsService.getHighEngagementRecipes(limit);

        return ResponseEntity.ok(recipes);
    }

    /**
     * Hiệu suất theo từng category
     */
    @GetMapping("/category-performance")
    public ResponseEntity<List<CategoryPerformanceDTO>> getCategoryPerformance() {

        List<CategoryPerformanceDTO> performance = statisticsService.getCategoryPerformance();

        return ResponseEntity.ok(performance);
    }

    /**
     * Thống kê độ hoàn thiện của công thức
     */
    @GetMapping("/completion-stats")
    public ResponseEntity<RecipeCompletionStatsDTO> getRecipeCompletionStats() {

        RecipeCompletionStatsDTO stats = statisticsService.getRecipeCompletionStats();

        return ResponseEntity.ok(stats);
    }
}