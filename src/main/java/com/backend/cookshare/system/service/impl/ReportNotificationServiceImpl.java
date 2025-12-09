package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.system.dto.request.NotificationMessage;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.service.ReportNotificationService;
import com.backend.cookshare.system.service.notification.builder.NotificationMessageBuilder;
import com.backend.cookshare.system.service.notification.persistence.NotificationPersistenceService;
import com.backend.cookshare.system.service.notification.resolver.ReportTargetResolver;
import com.backend.cookshare.system.service.notification.sender.WebSocketNotificationSender;
import com.backend.cookshare.user.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportNotificationServiceImpl implements ReportNotificationService {
    private final ReportQueryRepository queryRepository;
    private final NotificationMessageBuilder messageBuilder;
    private final WebSocketNotificationSender webSocketSender;
    private final NotificationPersistenceService persistenceService;
    private final ReportTargetResolver targetResolver;

    @Override
    public void notifyAdminsNewReport(Report report, String reporterUsername) {
        try {
            List<String> adminUsernames = queryRepository.findAdminUsernames();

            if (adminUsernames.isEmpty()) {
                log.warn("No active admins to notify about report {}", report.getReportId());
                return;
            }

            ReportTargetResolver.ReportTarget target = targetResolver.resolve(report);

            NotificationMessage message = messageBuilder.buildNewReportMessage(
                    report,
                    reporterUsername,
                    target.type(),
                    target.name()
            );

            webSocketSender.broadcastToUsers(adminUsernames, message);

            log.info("Notified {} admins about new report {}",
                    adminUsernames.size(), report.getReportId());

        } catch (Exception e) {
            log.error("Failed to notify admins about report {}", report.getReportId(), e);
        }
    }

    @Override
    public void notifyReporterReviewComplete(Report report, String reporterUsername, UUID reporterId) {
        try {
            String message = buildReviewCompleteMessage(report);

            // Save to database
            persistenceService.saveNotification(
                    reporterId,
                    "Kết quả xử lý báo cáo",
                    message,
                    NotificationType.REPORT_REVIEW,
                    report.getReportId()
            );

            // Send WebSocket
            NotificationMessage wsMessage = messageBuilder.buildReportReviewedMessage(
                    report,
                    reporterUsername
            );

            webSocketSender.sendToUser(reporterUsername, wsMessage);

            log.info("Notified reporter {} about review result for report {}",
                    reporterUsername, report.getReportId());

        } catch (Exception e) {
            log.error("Failed to notify reporter about review result", e);
        }
    }

    @Override
    public void broadcastPendingCountUpdate(long pendingCount) {
        try {
            List<String> adminUsernames = queryRepository.findAdminUsernames();

            NotificationMessage message = messageBuilder.buildPendingCountUpdateMessage(pendingCount);

            webSocketSender.broadcastToUsers(adminUsernames, message);

            log.debug("Broadcasted pending count update to {} admins", adminUsernames.size());
        } catch (Exception e) {
            log.error("Failed to broadcast pending count update", e);
        }
    }

    private String buildReviewCompleteMessage(Report report) {
        StringBuilder message = new StringBuilder();

        switch (report.getStatus()) {
            case APPROVED -> {
                message.append("Báo cáo của bạn đã được phê duyệt. ");
                if (report.getActionTaken() != null) {
                    message.append("Hành động đã thực hiện: ")
                            .append(report.getActionTaken().getDisplayName())
                            .append(". ");
                }
            }
            case REJECTED -> {
                message.append("Báo cáo của bạn đã được xem xét nhưng không đủ cơ sở để xử lý. ");
            }
            case RESOLVED -> {
                message.append("Báo cáo của bạn đã được giải quyết. ");
            }
            default -> message.append("Báo cáo của bạn đã được xử lý. ");
        }

        if (report.getActionDescription() != null) {
            message.append(report.getActionDescription());
        }

        return message.toString();
    }
}
