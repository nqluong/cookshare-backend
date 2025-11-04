package com.backend.cookshare.admin_report.controller;

import com.backend.cookshare.admin_report.dto.search_response.*;
import com.backend.cookshare.admin_report.service.SearchStatisticsService;
import com.backend.cookshare.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/admin/statistics/search")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SearchStatisticsController {

    private final SearchStatisticsService searchStatisticsService;

    /**
     * Lấy tổng quan thống kê tìm kiếm
     * GET /api/admin/statistics/search/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<SearchOverviewDTO>> getSearchOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy tổng quan thống kê tìm kiếm từ {} đến {}", startDate, endDate);

        SearchOverviewDTO overview = searchStatisticsService.getSearchOverview(startDate, endDate);

        ApiResponse<SearchOverviewDTO> response = ApiResponse.<SearchOverviewDTO>builder()
                .code(200)
                .message("Lấy tổng quan thống kê tìm kiếm thành công")
                .data(overview)
                .build();

        return ResponseEntity.ok(response);

    }

    /**
     * Lấy từ khóa tìm kiếm phổ biến nhất
     * GET /api/admin/statistics/search/popular-keywords
     */
    @GetMapping("/popular-keywords")
    public ResponseEntity<ApiResponse<PopularKeywordsDTO>> getPopularKeywords(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy top {} từ khóa phổ biến từ {} đến {}", limit, startDate, endDate);

        PopularKeywordsDTO keywords = searchStatisticsService.getPopularKeywords(limit, startDate, endDate);

        ApiResponse<PopularKeywordsDTO> response = ApiResponse.<PopularKeywordsDTO>builder()
                .code(200)
                .message("Lấy từ khóa phổ biến thành công")
                .data(keywords)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy nguyên liệu được tìm kiếm nhiều nhất
     * GET /api/admin/statistics/search/popular-ingredients
     */
    @GetMapping("/popular-ingredients")
    public ResponseEntity<ApiResponse<PopularIngredientsDTO>> getPopularIngredients(
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy top {} nguyên liệu phổ biến từ {} đến {}", limit, startDate, endDate);

        PopularIngredientsDTO ingredients = searchStatisticsService.getPopularIngredients(limit, startDate, endDate);

        ApiResponse<PopularIngredientsDTO> response = ApiResponse.<PopularIngredientsDTO>builder()
                .code(200)
                .message("Lấy nguyên liệu phổ biến thành công")
                .data(ingredients)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh mục được xem nhiều nhất
     * GET /api/admin/statistics/search/popular-categories
     */
    @GetMapping("/popular-categories")
    public ResponseEntity<ApiResponse<PopularCategoriesDTO>> getPopularCategories(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy danh mục phổ biến từ {} đến {}", startDate, endDate);

        PopularCategoriesDTO categories = searchStatisticsService.getPopularCategories(startDate, endDate);

        ApiResponse<PopularCategoriesDTO> response = ApiResponse.<PopularCategoriesDTO>builder()
                .code(200)
                .message("Lấy danh mục phổ biến thành công")
                .data(categories)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy tỷ lệ tìm kiếm thành công/không thành công
     * GET /api/admin/statistics/search/success-rate
     */
    @GetMapping("/success-rate")
    public ResponseEntity<ApiResponse<SearchSuccessRateDTO>> getSearchSuccessRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy tỷ lệ thành công tìm kiếm từ {} đến {}", startDate, endDate);

        SearchSuccessRateDTO successRate = searchStatisticsService.getSearchSuccessRate(startDate, endDate);

        ApiResponse<SearchSuccessRateDTO> response = ApiResponse.<SearchSuccessRateDTO>builder()
                .code(200)
                .message("Lấy tỷ lệ thành công tìm kiếm thành công")
                .data(successRate)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy từ khóa không có kết quả
     * GET /api/admin/statistics/search/zero-result-keywords
     */
    @GetMapping("/zero-result-keywords")
    public ResponseEntity<ApiResponse<ZeroResultKeywordsDTO>> getZeroResultKeywords(
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy top {} từ khóa không có kết quả từ {} đến {}", limit, startDate, endDate);

        ZeroResultKeywordsDTO keywords = searchStatisticsService.getZeroResultKeywords(limit, startDate, endDate);

        ApiResponse<ZeroResultKeywordsDTO> response = ApiResponse.<ZeroResultKeywordsDTO>builder()
                .code(200)
                .message("Lấy từ khóa không có kết quả thành công")
                .data(keywords)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy xu hướng tìm kiếm theo thời gian
     * GET /api/admin/statistics/search/trends
     */
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<SearchTrendsDTO>> getSearchTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "DAY") String groupBy) {

        log.info("Yêu cầu lấy xu hướng tìm kiếm từ {} đến {}, nhóm theo {}", startDate, endDate, groupBy);

        SearchTrendsDTO trends = searchStatisticsService.getSearchTrends(startDate, endDate, groupBy);
        ApiResponse<SearchTrendsDTO> response = ApiResponse.<SearchTrendsDTO>builder()
                .code(200)
                .message("Lấy xu hướng tìm kiếm thành công")
                .data(trends)
                .build();

        return ResponseEntity.ok(response);
    }
}
