package com.backend.cookshare.admin_report.controller;

import com.backend.cookshare.admin_report.dto.interaction_reponse.*;
import com.backend.cookshare.admin_report.dto.search_response.EngagementByCategoryDTO;
import com.backend.cookshare.admin_report.service.InteractionStatisticsService;
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
@RequestMapping("/api/admin/statistics/interaction")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InteractionStatisticsController {
    private final InteractionStatisticsService interactionStatisticsService;

    /**
     * Lấy tổng quan thống kê tương tác
     * GET /api/admin/statistics/interaction/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<InteractionOverviewDTO>> getInteractionOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy tổng quan thống kê tương tác từ {} đến {}", startDate, endDate);

        InteractionOverviewDTO overview = interactionStatisticsService.getInteractionOverview(startDate, endDate);

        ApiResponse<InteractionOverviewDTO> response = ApiResponse.<InteractionOverviewDTO>builder()
                .code(200)
                .message("Lấy tổng quan thống kê tương tác thành công")
                .data(overview)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thống kê tương tác chi tiết
     * GET /api/admin/statistics/interaction/detailed
     */
    @GetMapping("/detailed")
    public ResponseEntity<ApiResponse<DetailedInteractionStatsDTO>> getDetailedInteractionStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy thống kê tương tác chi tiết từ {} đến {}", startDate, endDate);


        DetailedInteractionStatsDTO stats = interactionStatisticsService.getDetailedInteractionStats(startDate, endDate);

        ApiResponse<DetailedInteractionStatsDTO> response = ApiResponse.<DetailedInteractionStatsDTO>builder()
                .code(200)
                .message("Lấy thống kê tương tác chi tiết thành công")
                .data(stats)
                .build();

        return ResponseEntity.ok(response);
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

        ApiResponse<PeakHoursStatsDTO> response = ApiResponse.<PeakHoursStatsDTO>builder()
                .code(200)
                .message("Lấy thống kê giờ cao điểm thành công")
                .data(stats)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy top bình luận được like nhiều nhất
     * GET /api/admin/statistics/interaction/top-comments
     */
    @GetMapping("/top-comments")
    public ResponseEntity<ApiResponse<TopCommentsDTO>> getTopComments(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy top {} bình luận từ {} đến {}", limit, startDate, endDate);

        TopCommentsDTO topComments = interactionStatisticsService.getTopComments(limit, startDate, endDate);

        ApiResponse<TopCommentsDTO> response = ApiResponse.<TopCommentsDTO>builder()
                .code(200)
                .message("Lấy top bình luận thành công")
                .data(topComments)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thống kê follow/unfollow theo thời gian
     * GET /api/admin/statistics/interaction/follow-trends
     */
    @GetMapping("/follow-trends")
    public ResponseEntity<ApiResponse<FollowTrendsDTO>> getFollowTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "DAY") String groupBy) {

        log.info("Yêu cầu lấy xu hướng follow từ {} đến {}, nhóm theo {}", startDate, endDate, groupBy);

        FollowTrendsDTO trends = interactionStatisticsService.getFollowTrends(startDate, endDate, groupBy);

        ApiResponse<FollowTrendsDTO> response = ApiResponse.<FollowTrendsDTO>builder()
                .code(200)
                .message("Lấy xu hướng follow thành công")
                .data(trends)
                .build();

        return ResponseEntity.ok(response);

    }

    /**
     * Lấy thống kê engagement rate theo danh mục
     * GET /api/admin/statistics/interaction/engagement-by-category
     */
    @GetMapping("/engagement-by-category")
    public ResponseEntity<ApiResponse<EngagementByCategoryDTO>> getEngagementByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Yêu cầu lấy engagement rate theo danh mục từ {} đến {}", startDate, endDate);

        EngagementByCategoryDTO engagement = interactionStatisticsService.getEngagementByCategory(startDate, endDate);

        ApiResponse<EngagementByCategoryDTO> response = ApiResponse.<EngagementByCategoryDTO>builder()
                .code(200)
                .message("Lấy engagement rate theo danh mục thành công")
                .data(engagement)
                .build();

        return ResponseEntity.ok(response);
    }
}
