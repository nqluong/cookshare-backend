package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.request.NotificationMessage;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.service.ReportNotificationService;
import com.backend.cookshare.system.service.notification.builder.NotificationMessageBuilder;
import com.backend.cookshare.system.service.notification.persistence.NotificationPersistenceService;
import com.backend.cookshare.system.service.notification.resolver.ReportTargetResolver;
import com.backend.cookshare.system.service.notification.sender.WebSocketNotificationSender;
import com.backend.cookshare.user.enums.NotificationType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportNotificationServiceImpl implements ReportNotificationService {

    ReportQueryRepository queryRepository;
    NotificationMessageBuilder messageBuilder;
    WebSocketNotificationSender webSocketSender;
    NotificationPersistenceService persistenceService;
    ReportTargetResolver targetResolver;

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

            log.info("Notified {} admins about new report {}", adminUsernames.size(), report.getReportId());

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

            log.info("Notified reporter {} about review result for report {}", reporterUsername, report.getReportId());

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

    @Override
    public void notifyAdminsActionCompleted(Report report) {
        try {
            List<String> adminUsernames = queryRepository.findAdminUsernames();

            if (adminUsernames.isEmpty()) {
                return;
            }

            String actionSummary = buildActionSummary(report);

            NotificationMessage message = NotificationMessage.builder()
                    .type("REPORT_ACTION_COMPLETED")
                    .title("Xử lý báo cáo hoàn tất")
                    .message(actionSummary)
                    .data(Map.of(
                            "reportId", report.getReportId().toString(),
                            "actionTaken", report.getActionTaken().toString(),
                            "status", report.getStatus().toString()
                    ))
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            webSocketSender.broadcastToUsers(adminUsernames, message);

            log.info("Notified {} admins about completed action for report {}",
                    adminUsernames.size(), report.getReportId());

        } catch (Exception e) {
            log.error("Failed to notify admins about completed action", e);
        }
    }

    @Override
    public void notifyUserWarned(Report report, UUID userId) {
        try {
            String username = queryRepository.findUsernameById(userId).orElseThrow(
                    () -> new CustomException(ErrorCode.USER_NOT_FOUND)
            );

            NotificationMessage message = messageBuilder.buildUserWarningMessage(
                    report.getReportType(),
                    "Cảnh báo"
            );

            // Save to database
            persistenceService.saveNotification(
                    userId,
                    message.getTitle(),
                    message.getMessage(),
                    NotificationType.WARNING, // Sử dụng WARNING cho cảnh báo
                    report.getReportId()
            );

            // Send WebSocket
            webSocketSender.sendToUser(username, message);

            log.info("Notified user {} about warning", username);

        } catch (Exception e) {
            log.error("Failed to notify user about warning", e);
        }
    }

    @Override
    public void notifyUserSuspended(Report report, UUID userId, int suspensionDays) {
        try {
            String username = queryRepository.findUsernameById(userId).orElseThrow(
                    () -> new CustomException(ErrorCode.USER_NOT_FOUND)
            );

            NotificationMessage message = messageBuilder.buildUserSuspendedMessage(suspensionDays);

            // Save to database
            persistenceService.saveNotification(
                    userId,
                    message.getTitle(),
                    message.getMessage(),
                    NotificationType.ACCOUNT_STATUS, // Cho suspend/ban
                    report.getReportId()
            );

            // Send WebSocket
            webSocketSender.sendToUser(username, message);

            log.info("Notified user {} about suspension", username);

        } catch (Exception e) {
            log.error("Failed to notify user about suspension", e);
        }
    }

    @Override
    public void notifyUserBanned(Report report, UUID userId) {
        try {
            String username = queryRepository.findUsernameById(userId).orElseThrow(
                    () -> new CustomException(ErrorCode.USER_NOT_FOUND)
            );

            NotificationMessage message = messageBuilder.buildUserBannedMessage();

            // Save to database
            persistenceService.saveNotification(
                    userId,
                    message.getTitle(),
                    message.getMessage(),
                    NotificationType.ACCOUNT_STATUS,
                    report.getReportId()
            );

            // Send WebSocket
            webSocketSender.sendToUser(username, message);

            log.info("Notified user {} about ban", username);

        } catch (Exception e) {
            log.error("Failed to notify user about ban", e);
        }
    }

    @Override
    public void notifyRecipeUnpublished(Report report, UUID recipeId) {
        try {
            // Lấy thông tin recipe và author
            RecipeInfo recipeInfo = queryRepository.findRecipeInfoById(recipeId);
            if (recipeInfo == null) {
                log.warn("Recipe not found: {}", recipeId);
                return;
            }

            String reason = report.getActionDescription() != null
                    ? report.getActionDescription()
                    : "Vi phạm chính sách cộng đồng";

            NotificationMessage message = messageBuilder.buildRecipeUnpublishedMessage(
                    recipeId,
                    recipeInfo.title(),
                    reason
            );

            // Save to database
            persistenceService.saveNotification(
                    recipeInfo.authorId(),
                    message.getTitle(),
                    message.getMessage(),
                    NotificationType.RECIPE_STATUS,
                    report.getReportId()
            );

            // Send WebSocket
            webSocketSender.sendToUser(recipeInfo.authorUsername(), message);

            log.info("Notified recipe author {} about unpublish", recipeInfo.authorUsername());

        } catch (Exception e) {
            log.error("Failed to notify about recipe unpublish", e);
        }
    }

    @Override
    public void notifyRecipeEditRequired(Report report, UUID recipeId) {
        try {
            // Lấy thông tin recipe và author
            RecipeInfo recipeInfo = queryRepository.findRecipeInfoById(recipeId);
            if (recipeInfo == null) {
                log.warn("Recipe not found: {}", recipeId);
                return;
            }

            String editRequirement = report.getActionDescription() != null
                    ? report.getActionDescription()
                    : "Vui lòng kiểm tra và chỉnh sửa nội dung";

            NotificationMessage message = messageBuilder.buildRecipeEditRequiredMessage(
                    recipeId,
                    recipeInfo.title(),
                    editRequirement
            );

            // Save to database
            persistenceService.saveNotification(
                    recipeInfo.authorId(),
                    message.getTitle(),
                    message.getMessage(),
                    NotificationType.RECIPE_STATUS,
                    report.getReportId()
            );

            // Send WebSocket
            webSocketSender.sendToUser(recipeInfo.authorUsername(), message);

            log.info("Notified recipe author {} about edit requirement", recipeInfo.authorUsername());

        } catch (Exception e) {
            log.error("Failed to notify about edit requirement", e);
        }
    }

    @Override
    public void notifyContentRemoved(Report report, UUID recipeId) {
        try {
            RecipeInfo recipeInfo = queryRepository.findRecipeInfoById(recipeId);
            if (recipeInfo == null) {
                log.warn("Recipe not found: {}", recipeId);
                return;
            }

            String reason = report.getActionDescription() != null
                    ? report.getActionDescription()
                    : "Nội dung vi phạm chính sách";

            NotificationMessage message = messageBuilder.buildRecipeUnpublishedMessage(
                    recipeId,
                    recipeInfo.title(),
                    reason
            );

            // Save to database
            persistenceService.saveNotification(
                    recipeInfo.authorId(),
                    "Nội dung đã bị xóa",
                    message.getMessage(),
                    NotificationType.RECIPE_STATUS,
                    report.getReportId()
            );

            // Send WebSocket
            webSocketSender.sendToUser(recipeInfo.authorUsername(), message);

            log.info("Notified recipe author {} about content removal", recipeInfo.authorUsername());

        } catch (Exception e) {
            log.error("Failed to notify about content removal", e);
        }
    }

    // Helper methods

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

    private String buildActionSummary(Report report) {
        String actionName = report.getActionTaken().getDisplayName();
        String targetInfo = "";

        if (report.getRecipeId() != null) {
            RecipeInfo recipe = queryRepository.findRecipeInfoById(report.getRecipeId());
            if (recipe != null) {
                targetInfo = String.format("công thức '%s'", recipe.title());
            }
        } else if (report.getReportedId() != null) {

            String username = queryRepository.findUsernameById(report.getReporterId()).orElseThrow(
                    () -> new CustomException(ErrorCode.USER_NOT_FOUND)
            );
        }

        return String.format("Báo cáo #%s: Đã thực hiện '%s' đối với %s",
                report.getReportId().toString().substring(0, 8),
                actionName,
                targetInfo);
    }

    // DTO for recipe info
    public record RecipeInfo(UUID recipeId, String title, UUID authorId, String authorUsername) {}
}