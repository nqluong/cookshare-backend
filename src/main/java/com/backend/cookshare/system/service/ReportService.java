package com.backend.cookshare.system.service;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.system.dto.request.CreateReportRequest;
import com.backend.cookshare.system.dto.request.ReportFilterRequest;
import com.backend.cookshare.system.dto.request.ReviewReportRequest;
import com.backend.cookshare.system.dto.response.*;

import java.util.UUID;

public interface ReportService {
    /**
     * Người dùng tạo báo cáo mới
     */
    ReportResponse createReport(CreateReportRequest request);

    /**
     * Admin xem danh sách reports (flat list - individual reports)
     */
    PageResponse<ReportResponse> getReports(ReportFilterRequest filter);

    /**
     * Admin xem chi tiết 1 report
     */
    ReportResponse getReportById(UUID reportId);

    /**
     * Admin review 1 report
     */
    ReportResponse reviewReport(UUID reportId, ReviewReportRequest request);

    /**
     * Admin xóa 1 report
     */
    void deleteReport(UUID reportId);

    /**
     * Admin xem danh sách grouped by target (recipes/users bị báo cáo)
     * Đây là view chính, recommended cho admin
     */
    PageResponse<ReportGroupResponse> getGroupedReports(int page, int size);

    /**
     * Admin xem chi tiết 1 target bị báo cáo (tất cả reports của target đó)
     */
    ReportGroupDetailResponse getGroupDetail(String targetType, UUID targetId);

    /**
     * Admin review tất cả reports của 1 target cùng lúc
     * Batch action - apply cùng 1 action cho all reports của target
     */
    BatchReviewResponse batchReviewByTarget(
            String targetType,
            UUID targetId,
            ReviewReportRequest request
    );


    /**
     * Lấy thống kê tổng quan
     */
    ReportStatisticsResponse getStatistics();

    /**
     * Lấy thống kê theo target (bao nhiêu recipes bị report, bao nhiêu users)
     */
    TargetStatisticsResponse getTargetStatistics();
}
