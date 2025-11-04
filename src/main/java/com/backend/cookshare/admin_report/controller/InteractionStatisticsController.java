package com.backend.cookshare.admin_report.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/admin-report/interaction-statistics")
@RequiredArgsConstructor
public class InteractionStatisticsController {
    private final InteractionStatisticsService interactionStatisticsService;

    /**
     * Lấy tổng quan thống kê tương tác
     * GET /api/v1/admin/statistics/interaction/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<InteractionOverviewDTO>> getInteractionOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy tổng quan thống kê tương tác từ {} đến {}", startDate, endDate);

        InteractionOverviewDTO overview = interactionStatisticsService.getInteractionOverview(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(overview, "Lấy tổng quan thống kê tương tác thành công"));
    }

    /**
     * Lấy thống kê tương tác chi tiết
     * GET /api/v1/admin/statistics/interaction/detailed
     */
    @GetMapping("/detailed")
    public ResponseEntity<ApiResponse<DetailedInteractionStatsDTO>> getDetailedInteractionStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy thống kê tương tác chi tiết từ {} đến {}", startDate, endDate);

        DetailedInteractionStatsDTO stats = interactionStatisticsService.getDetailedInteractionStats(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(stats, "Lấy thống kê tương tác chi tiết thành công"));
    }

    /**
     * Lấy thống kê theo giờ trong ngày
     * GET /api/v1/admin/statistics/interaction/peak-hours
     */
    @GetMapping("/peak-hours")
    public ResponseEntity<ApiResponse<PeakHoursStatsDTO>> getPeakHoursStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy thống kê giờ cao điểm từ {} đến {}", startDate, endDate);

        PeakHoursStatsDTO stats = interactionStatisticsService.getPeakHoursStats(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(stats, "Lấy thống kê giờ cao điểm thành công"));
    }

    /**
     * Lấy top bình luận được like nhiều nhất
     * GET /api/v1/admin/statistics/interaction/top-comments
     */
    @GetMapping("/top-comments")
    public ResponseEntity<ApiResponse<TopCommentsDTO>> getTopComments(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy top {} bình luận từ {} đến {}", limit, startDate, endDate);

        TopCommentsDTO topComments = interactionStatisticsService.getTopComments(limit, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(topComments, "Lấy top bình luận thành công"));
    }

    /**
     * Lấy thống kê follow/unfollow theo thời gian
     * GET /api/v1/admin/statistics/interaction/follow-trends
     */
    @GetMapping("/follow-trends")
    public ResponseEntity<ApiResponse<FollowTrendsDTO>> getFollowTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "DAY") String groupBy) {

        log.info("Yêu cầu lấy xu hướng follow từ {} đến {}, nhóm theo {}", startDate, endDate, groupBy);

        FollowTrendsDTO trends = interactionStatisticsService.getFollowTrends(startDate, endDate, groupBy);

        return ResponseEntity.ok(ApiResponse.success(trends, "Lấy xu hướng follow thành công"));
    }

    /**
     * Lấy thống kê engagement rate theo danh mục
     * GET /api/v1/admin/statistics/interaction/engagement-by-category
     */
    @GetMapping("/engagement-by-category")
    public ResponseEntity<ApiResponse<EngagementByCategoryDTO>> getEngagementByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy engagement rate theo danh mục từ {} đến {}", startDate, endDate);

        EngagementByCategoryDTO engagement = interactionStatisticsService.getEngagementByCategory(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(engagement, "Lấy engagement rate theo danh mục thành công"));
    }
}
