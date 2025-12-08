package com.backend.cookshare.system.service;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.system.dto.request.CreateReportRequest;
import com.backend.cookshare.system.dto.request.ReportFilterRequest;
import com.backend.cookshare.system.dto.request.ReviewReportRequest;
import com.backend.cookshare.system.dto.response.ReportResponse;
import com.backend.cookshare.system.dto.response.ReportStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ReportService {
    ReportResponse createReport(CreateReportRequest request);

    PageResponse<ReportResponse> getReports(ReportFilterRequest filter);

    ReportResponse getReportById(UUID reportId);

    ReportResponse reviewReport(UUID reportId, ReviewReportRequest request);

    void deleteReport(UUID reportId);

    ReportStatisticsResponse getStatistics();
}
