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

        relatedReports.forEach(r -> {
            if (!r.getReportId().equals(reviewedReport.getReportId())) {
                r.setStatus(reviewedReport.getStatus());
                r.setActionTaken(reviewedReport.getActionTaken());
                r.setActionDescription(reviewedReport.getActionDescription());
                r.setReviewedBy(reviewedReport.getReviewedBy());
                r.setReviewedAt(reviewedReport.getReviewedAt());
            }
        });

        reportRepository.saveAll(relatedReports);

        log.info("Synced {} related reports for report {}",
                relatedReports.size() - 1, reviewedReport.getReportId());
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
