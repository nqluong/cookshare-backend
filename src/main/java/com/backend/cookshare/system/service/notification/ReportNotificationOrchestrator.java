package com.backend.cookshare.system.service.notification;

import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.ReportRepository;
import com.backend.cookshare.system.repository.projection.UsernameProjection;
import com.backend.cookshare.system.service.ReportNotificationService;
import com.backend.cookshare.system.service.sync.ReportSynchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ReportNotificationOrchestrator {

    private final ReportNotificationService notificationService;
    private final ReportQueryRepository reportQueryRepository;
    private final ReportRepository reportRepository;
    private final ReportSynchronizer reportSynchronizer;
    private final Executor asyncExecutor;

    public ReportNotificationOrchestrator(ReportNotificationService notificationService,
                                          ReportQueryRepository reportQueryRepository,
                                          ReportRepository reportRepository,
                                          ReportSynchronizer reportSynchronizer,
                                          @Qualifier("reportAsyncExecutor") Executor asyncExecutor) {
        this.notificationService = notificationService;
        this.reportQueryRepository = reportQueryRepository;
        this.reportRepository = reportRepository;
        this.reportSynchronizer = reportSynchronizer;
        this.asyncExecutor = asyncExecutor;
    }

    public void notifyAllReportersAsync(Report reviewedReport) {
        CompletableFuture.runAsync(() -> {
            try {
                notifyAllReporters(reviewedReport);
            } catch (Exception e) {
                log.error("Lỗi khi thông báo cho người báo cáo: {}", e.getMessage(), e);
            }
        }, asyncExecutor);
    }

    private void notifyAllReporters(Report reviewedReport) {
        List<Report> relatedReports = reportSynchronizer.findRelatedReports(reviewedReport);

        if (relatedReports.isEmpty()) {
            log.warn("Không tìm thấy báo cáo liên quan cho báo cáo {}", reviewedReport.getReportId());
            return;
        }

        // Chỉ lấy những báo cáo CHƯA được thông báo trước đó
        // Tránh gửi lại thông báo cho những người đã được xử lý với NO_ACTION hoặc action khác
        List<Report> unnotifiedReports = relatedReports.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getReportersNotified()))
                .collect(Collectors.toList());

        if (unnotifiedReports.isEmpty()) {
            log.info("Tất cả người báo cáo đã được thông báo trước đó cho báo cáo {}", 
                    reviewedReport.getReportId());
            return;
        }

        List<UUID> reporterIds = unnotifiedReports.stream()
                .map(Report::getReporterId)
                .distinct()
                .collect(Collectors.toList());

        log.info("Đang thông báo {} người báo cáo (chưa được thông báo) về kết quả xem xét báo cáo {}",
                reporterIds.size(), reviewedReport.getReportId());

        List<UsernameProjection> reporters = reportQueryRepository.findUsernamesByIds(reporterIds);

        for (UsernameProjection reporter : reporters) {
            try {
                notificationService.notifyReporterReviewComplete(
                        reviewedReport,
                        reporter.getUsername(),
                        reporter.getUserId()
                );
            } catch (Exception e) {
                log.error("Không thể thông báo cho người báo cáo {}: {}",
                        reporter.getUserId(), e.getMessage());
            }
        }

        // Chỉ đánh dấu những báo cáo chưa được thông báo
        markReportsAsNotified(unnotifiedReports);

        log.info("Đã thông báo thành công {} người báo cáo mới", reporters.size());
    }

    private void markReportsAsNotified(List<Report> reports) {
        reports.forEach(r -> r.setReportersNotified(true));
        reportRepository.saveAll(reports);
    }
}
