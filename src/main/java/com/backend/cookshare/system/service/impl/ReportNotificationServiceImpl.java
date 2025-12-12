package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.request.NotificationMessage;
import com.backend.cookshare.system.dto.response.RecipeInfo;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.projection.UsernameProjection;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    NotificationPersistenceService persistenceService;
    ReportTargetResolver targetResolver;
    SimpMessagingTemplate messagingTemplate; // ⬅️ STOMP sender mới

    /** Gửi STOMP WebSocket đến 1 user */
    private void sendToUser(String username, NotificationMessage message) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                message
        );
    }

    /** Broadcast cho nhiều user */
    private void broadcastToUsers(List<String> usernames, NotificationMessage message) {
        for (String username : usernames) {
            sendToUser(username, message);
        }
    }

    @Override
    public void notifyRecipeUnpublished(Report report, UUID recipeId) {
    }

    @Override
    public void notifyAdminsNewReport(Report report, String reporterUsername) {
        try {
            List<String> admins = queryRepository.findAdminUsernames();
            if (admins.isEmpty()) {
                return;
            }

            var target = targetResolver.resolve(report);

            NotificationMessage msg = messageBuilder.buildNewReportMessage(
                    report,
                    reporterUsername,
                    target.type(),
                    target.name()
            );

            broadcastToUsers(admins, msg);

        } catch (Exception e) {
            log.error("Không gửi được thông báo báo cáo mới", e);
        }
    }

    @Override
    public void notifyReporterReviewComplete(Report report, String reporterUsername, UUID reporterId) {
        try {
            String msgText = buildReviewCompleteMessage(report);

            // Lưu DB
            persistenceService.saveNotification(
                    reporterId,
                    "Kết quả xử lý báo cáo",
                    msgText,
                    NotificationType.REPORT_REVIEW,
                    report.getReportId()
            );

            // Gửi WS
            NotificationMessage ws = messageBuilder.buildReportReviewedMessage(report, reporterUsername);
            sendToUser(reporterUsername, ws);

        } catch (Exception e) {
            log.error("Không gửi notify reporter review", e);
        }
    }

    @Override
    public void broadcastPendingCountUpdate(long pendingCount) {
        try {
            List<String> admins = queryRepository.findAdminUsernames();
            NotificationMessage msg = messageBuilder.buildPendingCountUpdateMessage(pendingCount);
            broadcastToUsers(admins, msg);
        } catch (Exception e) {
            log.error("Không broadcast số lượng pending reports", e);
        }
    }

    @Override
    public void notifyAdminsActionCompleted(Report report) {
        try {
            List<String> admins = queryRepository.findAdminUsernames();
            if (admins.isEmpty()) return;

            String summary = buildActionSummary(report);

            NotificationMessage msg = NotificationMessage.builder()
                    .type("REPORT_ACTION_COMPLETED")
                    .title("Xử lý báo cáo hoàn tất")
                    .message(summary)
                    .data(Map.of(
                            "reportId", report.getReportId().toString(),
                            "actionTaken", report.getActionTaken().toString(),
                            "status", report.getStatus().toString()
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();

            broadcastToUsers(admins, msg);

        } catch (Exception e) {
            log.error("Không broadcast action completed", e);
        }
    }

    @Override
    public void notifyUserWarned(Report report, UUID userId) {
        try {
            String username = findUsernameById(userId);

            NotificationMessage msg = messageBuilder.buildUserWarningMessage(
                    report.getReportType(),
                    "Cảnh báo"
            );

            persistenceService.saveNotification(
                    userId,
                    msg.getTitle(),
                    msg.getMessage(),
                    NotificationType.WARNING,
                    report.getReportId()
            );

            sendToUser(username, msg);

        } catch (Exception e) {
            log.error("Không notify warning user", e);
        }
    }

    @Override
    public void notifyUserSuspended(Report report, UUID userId, int suspensionDays) {
        try {
            String username = findUsernameById(userId);

            NotificationMessage msg = messageBuilder.buildUserSuspendedMessage(suspensionDays);

            persistenceService.saveNotification(
                    userId,
                    msg.getTitle(),
                    msg.getMessage(),
                    NotificationType.ACCOUNT_STATUS,
                    report.getReportId()
            );

            sendToUser(username, msg);

        } catch (Exception e) {
            log.error("Không notify suspended user", e);
        }
    }

    @Override
    public void notifyUserBanned(Report report, UUID userId) {
        try {
            String username = findUsernameById(userId);

            NotificationMessage msg = messageBuilder.buildUserBannedMessage();

            persistenceService.saveNotification(
                    userId,
                    msg.getTitle(),
                    msg.getMessage(),
                    NotificationType.ACCOUNT_STATUS,
                    report.getReportId()
            );

            sendToUser(username, msg);

        } catch (Exception e) {
            log.error("Không notify banned user", e);
        }
    }

    @Override
    public void notifyRecipeEditRequired(Report report, UUID recipeId) {
        try {
            RecipeInfo info = queryRepository.findRecipeInfoById(recipeId).orElse(null);
            if (info == null) return;

            String note = report.getActionDescription() != null
                    ? report.getActionDescription()
                    : "Vui lòng chỉnh sửa nội dung";

            NotificationMessage msg = messageBuilder.buildRecipeEditRequiredMessage(
                    recipeId, info.getTitle(), note
            );

            persistenceService.saveNotification(
                    info.getAuthorId(),
                    msg.getTitle(),
                    msg.getMessage(),
                    NotificationType.RECIPE_STATUS,
                    report.getReportId()
            );

            sendToUser(info.getAuthorUsername(), msg);

        } catch (Exception e) {
            log.error("Không notify recipe edit required", e);
        }
    }

    @Override
    public void notifyContentRemoved(Report report, UUID recipeId) {
        try {
            RecipeInfo info = queryRepository.findRecipeInfoById(recipeId).orElse(null);
            if (info == null) return;

            String reason = report.getActionDescription() != null
                    ? report.getActionDescription()
                    : "Nội dung vi phạm chính sách";

            NotificationMessage msg = messageBuilder.buildRecipeUnpublishedMessage(
                    recipeId, info.getTitle(), reason
            );

            persistenceService.saveNotification(
                    info.getAuthorId(),
                    "Nội dung đã bị xóa",
                    msg.getMessage(),
                    NotificationType.RECIPE_STATUS,
                    report.getReportId()
            );

            sendToUser(info.getAuthorUsername(), msg);

        } catch (Exception e) {
            log.error("Không notify content removed", e);
        }
    }

    @Override
    public void notifyAutoDisableUser(UUID userId, String username, long reportCount) {
        try {
            NotificationMessage msg = messageBuilder.buildAutoDisableUserMessage(reportCount);

            persistenceService.saveNotification(
                    userId,
                    msg.getTitle(),
                    msg.getMessage(),
                    NotificationType.ACCOUNT_STATUS,
                    null
            );

            sendToUser(username, msg);

        } catch (Exception e) {
            log.error("Không notify auto disable user", e);
        }
    }

    @Override
    public void notifyAutoUnpublishRecipe(UUID recipeId, UUID authorId, String authorUsername, String recipeTitle, long reportCount) {
        try {
            NotificationMessage msg = messageBuilder.buildAutoUnpublishRecipeMessage(
                    recipeId, recipeTitle, reportCount
            );

            persistenceService.saveNotification(
                    authorId,
                    msg.getTitle(),
                    msg.getMessage(),
                    NotificationType.RECIPE_STATUS,
                    null
            );

            sendToUser(authorUsername, msg);

        } catch (Exception e) {
            log.error("Không notify auto unpublish recipe", e);
        }
    }

    /* ================== Helper ================== */

    private String buildReviewCompleteMessage(Report report) {
        StringBuilder sb = new StringBuilder();

        switch (report.getStatus()) {
            case APPROVED -> sb.append("Báo cáo đã được phê duyệt. ");
            case REJECTED -> sb.append("Báo cáo không đủ cơ sở xử lý. ");
            case RESOLVED -> sb.append("Báo cáo đã được giải quyết. ");
            default -> sb.append("Báo cáo đã được xử lý. ");
        }

        if (report.getActionDescription() != null) {
            sb.append(report.getActionDescription());
        }
        return sb.toString();
    }

    private String buildActionSummary(Report report) {
        String action = report.getActionTaken().getDisplayName();
        String target = "";

        if (report.getRecipeId() != null) {
            RecipeInfo info = queryRepository.findRecipeInfoById(report.getRecipeId()).orElse(null);
            if (info != null) {
                target = "công thức '" + info.getTitle() + "'";
            }
        } else if (report.getReportedId() != null) {
            String username = findUsernameById(report.getReportedId());
            target = "người dùng '" + username + "'";
        }

        return String.format(
                "Báo cáo #%s: Đã thực hiện '%s' đối với %s",
                report.getReportId().toString().substring(0, 8),
                action,
                target
        );
    }

    private String findUsernameById(UUID userId) {
        var list = queryRepository.findUsernamesByIds(List.of(userId));
        if (list.isEmpty()) throw new CustomException(ErrorCode.USER_NOT_FOUND);
        return list.get(0).getUsername();
    }
}
