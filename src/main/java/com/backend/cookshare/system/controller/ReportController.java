package com.backend.cookshare.system.controller;

import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.dto.ApiResponse;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.request.CreateReportRequest;
import com.backend.cookshare.system.dto.request.ReportFilterRequest;
import com.backend.cookshare.system.dto.request.ReviewReportRequest;
import com.backend.cookshare.system.dto.response.ReportResponse;
import com.backend.cookshare.system.dto.response.ReportStatisticsResponse;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final SecurityUtil securityUtil;
    private final ReportQueryRepository reportQueryRepository;

    /**
     * User tạo báo cáo mới
     * User ID được lấy tự động từ SecurityContextHolder
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @Valid @RequestBody CreateReportRequest request) {

        ReportResponse response = reportService.createReport(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ReportResponse>builder()
                        .success(true)
                        .message("Tạo báo cáo thành công")
                        .data(response)
                        .build());
    }

    /**
     * Admin xem danh sách báo cáo với filter
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> getReports(
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) UUID reporterId,
            @RequestParam(required = false) UUID reportedId,
            @RequestParam(required = false) UUID recipeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        ReportFilterRequest filter = ReportFilterRequest.builder()
                .reportType(reportType)
                .status(status)
                .reporterId(reporterId)
                .reportedId(reportedId)
                .recipeId(recipeId)
                .fromDate(fromDate)
                .toDate(toDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<ReportResponse> reports = reportService.getReports(filter);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReportResponse>>builder()
                .success(true)
                .message("Lấy danh sách báo cáo thành công")
                .data(reports)
                .build());
    }

    /**
     * Admin xem chi tiết báo cáo
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(
            @PathVariable UUID reportId) {

        ReportResponse report = reportService.getReportById(reportId);

        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder()
                .success(true)
                .message("Lấy chi tiết báo cáo thành công")
                .data(report)
                .build());
    }

    /**
     * Admin phê duyệt/từ chối báo cáo
     * Admin ID được lấy tự động từ SecurityContextHolder
     */
    @PatchMapping("/{reportId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> reviewReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewReportRequest request) {

        ReportResponse response = reportService.reviewReport(reportId, request);

        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder()
                .success(true)
                .message("Xem xét báo cáo thành công")
                .data(response)
                .build());
    }

    /**
     * Admin xóa báo cáo
     */
    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable UUID reportId) {
        reportService.deleteReport(reportId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa báo cáo thành công")
                .build());
    }

    /**
     * Admin xem thống kê báo cáo
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportStatisticsResponse>> getStatistics() {
        ReportStatisticsResponse stats = reportService.getStatistics();

        return ResponseEntity.ok(ApiResponse.<ReportStatisticsResponse>builder()
                .success(true)
                .message("Lấy thống kê thành công")
                .data(stats)
                .build());
    }

    /**
     * User xem các báo cáo của mình
     * User ID được lấy tự động từ SecurityContextHolder
     */
    @GetMapping("/my-reports")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> getMyReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Lấy current user ID từ SecurityContextHolder trong service
        String username = securityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_AUTHENTICATED));

        UUID reporterId = reportQueryRepository.findUserIdByUsername(username)
                .orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));

        ReportFilterRequest filter = ReportFilterRequest.builder()
                .reporterId(reporterId)
                .status(status)
                .page(page)
                .size(size)
                .sortBy("createdAt")
                .sortDirection("DESC")
                .build();

        PageResponse<ReportResponse> reports = reportService.getReports(filter);

        return ResponseEntity.ok(ApiResponse.<PageResponse<ReportResponse>>builder()
                .success(true)
                .message("Lấy báo cáo của bạn thành công")
                .data(reports)
                .build());
    }

    /**
     * Lấy số lượng pending reports (cho admin dashboard)
     */
    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPendingCount() {
        ReportStatisticsResponse stats = reportService.getStatistics();

        Map<String, Long> result = Map.of(
                "pendingCount", stats.getPendingReports(),
                "totalCount", stats.getTotalReports()
        );

        return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                .success(true)
                .message("Lấy số lượng báo cáo chờ xử lý thành công")
                .data(result)
                .build());
    }

}
