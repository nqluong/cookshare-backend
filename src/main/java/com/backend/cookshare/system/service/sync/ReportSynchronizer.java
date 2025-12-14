package com.backend.cookshare.system.service.sync;

import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportSynchronizer {
    private final ReportRepository reportRepository;

    public void syncRelatedReports(Report reviewedReport) {
        List<Report> relatedReports = findRelatedReports(reviewedReport);

        if (relatedReports.isEmpty()) {
            return;
        }

        // Chỉ sync những báo cáo PENDING (chưa được xử lý)
        // Không ghi đè lên những báo cáo đã được xử lý trước đó (VD: NO_ACTION)
        List<Report> pendingReports = relatedReports.stream()
                .filter(r -> !r.getReportId().equals(reviewedReport.getReportId()))
                .filter(r -> r.getStatus() == com.backend.cookshare.system.enums.ReportStatus.PENDING)
                .toList();

        if (pendingReports.isEmpty()) {
            log.info("Không có báo cáo PENDING nào cần sync cho báo cáo {}", reviewedReport.getReportId());
            return;
        }

        pendingReports.forEach(r -> {
            r.setStatus(reviewedReport.getStatus());
            r.setActionTaken(reviewedReport.getActionTaken());
            r.setActionDescription(reviewedReport.getActionDescription());
            r.setReviewedBy(reviewedReport.getReviewedBy());
            r.setReviewedAt(reviewedReport.getReviewedAt());
        });

        reportRepository.saveAll(pendingReports);

        log.info("Đã sync {} báo cáo PENDING liên quan cho báo cáo {}", 
                pendingReports.size(), reviewedReport.getReportId());
    }

    public List<Report> findRelatedReports(Report report) {
        if (report.getRecipeId() != null) {
            return reportRepository.findAllByRecipeId(report.getRecipeId());
        } else if (report.getReportedId() != null) {
            return reportRepository.findAllByReportedUserId(report.getReportedId());
        }
        return Collections.emptyList();
    }
}
