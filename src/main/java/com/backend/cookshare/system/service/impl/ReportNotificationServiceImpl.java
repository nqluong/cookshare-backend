package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.request.NotificationMessage;
import com.backend.cookshare.system.dto.response.RecipeInfo;
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
    public void notifyRecipeUnpublished(Report report, UUID recipeId) {

    }

    @Override
    public void notifyAdminsNewReport(Report report, String reporterUsername) {
        try {
            List<String> adminUsernames = queryRepository.findAdminUsernames();

            if (adminUsernames.isEmpty()) {
                log.warn("Không có quản trị viên nào đang hoạt động để thông báo về báo cáo {}", report.getReportId());
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

            log.info("Đã thông báo {} quản trị viên về báo cáo mới {}", adminUsernames.size(), report.getReportId());

        } catch (Exception e) {
            log.error("Không thể thông báo cho quản trị viên về báo cáo {}", report.getReportId(), e);
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

            log.info("Đã thông báo người báo cáo {} về kết quả xem xét cho báo cáo {}", reporterUsername, report.getReportId());

        } catch (Exception e) {
            log.error("Không thể thông báo cho người báo cáo về kết quả xem xét", e);
        }
    }

    @Override
    public void broadcastPendingCountUpdate(long pendingCount) {
        try {
            List<String> adminUsernames = queryRepository.findAdminUsernames();

            NotificationMessage message = messageBuilder.buildPendingCountUpdateMessage(pendingCount);

            webSocketSender.broadcastToUsers(adminUsernames, message);

            log.debug("Đã phát cập nhật số lượng báo cáo chờ xử lý đến {} quản trị viên", adminUsernames.size());
        } catch (Exception e) {
            log.error("Không thể phát cập nhật số lượng báo cáo chờ xử lý", e);
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

            log.info("Đã thông báo {} quản trị viên về hành động đã hoàn thành cho báo cáo {}",
                    adminUsernames.size(), report.getReportId());

        } catch (Exception e) {
            log.error("Không thể thông báo cho quản trị viên về hành động đã hoàn thành", e);
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

            log.info("Đã thông báo người dùng {} về cảnh báo", username);

        } catch (Exception e) {
            log.error("Không thể thông báo cho người dùng về cảnh báo", e);
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

            log.info("Đã thông báo người dùng {} về việc bị tạm khóa", username);

        } catch (Exception e) {
            log.error("Không thể thông báo cho người dùng về việc bị tạm khóa", e);
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

            log.info("Đã thông báo người dùng {} về việc bị cấm", username);

        } catch (Exception e) {
            log.error("Không thể thông báo cho người dùng về việc bị cấm", e);
        }
    }

    @Override
    public void notifyRecipeEditRequired(Report report, UUID recipeId) {
        try {
            // Lấy thông tin recipe và author
            RecipeInfo recipeInfo = queryRepository.findRecipeInfoById(recipeId);
            if (recipeInfo == null) {
                log.warn("Không tìm thấy công thức: {}", recipeId);
                return;
            }

            String editRequirement = report.getActionDescription() != null
                    ? report.getActionDescription()
                    : "Vui lòng kiểm tra và chỉnh sửa nội dung";

            NotificationMessage message = messageBuilder.buildRecipeEditRequiredMessage(
                    recipeId,
                    recipeInfo.getTitle(),
                    editRequirement
            );

            // Save to database
            persistenceService.saveNotification(
                    recipeInfo.getAuthorId(),
                    message.getTitle(),
                    message.getMessage(),
                    NotificationType.RECIPE_STATUS,
                    report.getReportId()
            );

            // Send WebSocket
            webSocketSender.sendToUser(recipeInfo.getAuthorUsername(), message);

            log.info("Đã thông báo tác giả công thức {} về yêu cầu chỉnh sửa", recipeInfo.getAuthorUsername());

        } catch (Exception e) {
            log.error("Không thể thông báo về yêu cầu chỉnh sửa", e);
        }
    }

    @Override
    public void notifyContentRemoved(Report report, UUID recipeId) {
        try {
            RecipeInfo recipeInfo = queryRepository.findRecipeInfoById(recipeId);
            if (recipeInfo == null) {
                log.warn("Không tìm thấy công thức: {}", recipeId);
                return;
            }

            String reason = report.getActionDescription() != null
                    ? report.getActionDescription()
                    : "Nội dung vi phạm chính sách";

            NotificationMessage message = messageBuilder.buildRecipeUnpublishedMessage(
                    recipeId,
                    recipeInfo.getTitle(),
                    reason
            );

            // Save to database
            persistenceService.saveNotification(
                    recipeInfo.getAuthorId(),
                    "Nội dung đã bị xóa",
                    message.getMessage(),
                    NotificationType.RECIPE_STATUS,
                    report.getReportId()
            );

            // Send WebSocket
            webSocketSender.sendToUser(recipeInfo.getAuthorUsername(), message);

            log.info("Đã thông báo tác giả công thức {} về việc xóa nội dung", recipeInfo.getAuthorUsername());

        } catch (Exception e) {
            log.error("Không thể thông báo về việc xóa nội dung", e);
        }
    }

    @Override
    public void notifyAutoDisableUser(UUID userId, String username, long reportCount) {
        try {
            NotificationMessage message = messageBuilder.buildAutoDisableUserMessage(reportCount);

            // Save to database
            persistenceService.saveNotification(
                    userId,
                    message.getTitle(),
                    message.getMessage(),
                    NotificationType.ACCOUNT_STATUS,
                    null // Không có reportId cụ thể vì là tổng hợp nhiều reports
            );

            // Send WebSocket
            webSocketSender.sendToUser(username, message);

            log.warn("Notified user {} about auto-disable due to {} reports", username, reportCount);

        } catch (Exception e) {
            log.error("Failed to notify user about auto-disable", e);
        }
    }

    @Override
    public void notifyAutoUnpublishRecipe(UUID recipeId, UUID authorId, String authorUsername, String recipeTitle, long reportCount) {
        try {
            NotificationMessage message = messageBuilder.buildAutoUnpublishRecipeMessage(
                    recipeId,
                    recipeTitle,
                    reportCount
            );

            // Save to database
            persistenceService.saveNotification(
                    authorId,
                    message.getTitle(),
                    message.getMessage(),
                    NotificationType.RECIPE_STATUS,
                    null // Không có reportId cụ thể vì là tổng hợp nhiều reports
            );

            // Send WebSocket
            webSocketSender.sendToUser(authorUsername, message);

            log.warn("Notified recipe author {} about auto-unpublish of '{}' due to {} reports",
                    authorUsername, recipeTitle, reportCount);

        } catch (Exception e) {
            log.error("Failed to notify about auto-unpublish", e);
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
                targetInfo = String.format("công thức '%s'", recipe.getTitle());
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

}