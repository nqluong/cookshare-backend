package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.request.NewReportNotification;
import com.backend.cookshare.system.dto.request.NotificationMessage;
import com.backend.cookshare.system.dto.response.ReportReviewNotification;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.service.ReportNotificationService;
import com.backend.cookshare.user.entity.Notification;
import com.backend.cookshare.user.enums.NotificationType;
import com.backend.cookshare.user.repository.NotificationRepository;
import com.backend.cookshare.user.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
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
public class ReportNotificationServiceImpl implements ReportNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ReportQueryRepository queryRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public void notifyReporterReviewComplete(Report report, String reporterUsername, UUID reporterId) {
        String title = "Kết quả xử lý báo cáo";
        String message = buildReviewCompleteMessage(report);

        // Save notification to database
        Notification notification = Notification.builder()
                .userId(reporterId)
                .title(title)
                .message(message)
                .type(NotificationType.REPORT_REVIEW)
                .relatedId(report.getReportId())
                .build();

        notificationRepository.save(notification);

        // Send WebSocket notification
        ReportReviewNotification wsNotification = ReportReviewNotification.builder()
                .reportId(report.getReportId())
                .status(report.getStatus())
                .actionTaken(report.getActionTaken())
                .actionDescription(report.getActionDescription())
                .reviewedAt(report.getReviewedAt())
                .message(message)
                .build();

        messagingTemplate.convertAndSendToUser(
                reporterUsername,
                "/queue/report-review",
                wsNotification
        );

        log.info("Đã gửi thông báo kết quả review report {} cho {}",
                report.getReportId(), reporterUsername);
    }

    @Override
    public void notifyReportedUser(UUID userId, String username, String actionDescription) {

    }

    @Override
    public void notifyRecipeAuthorUnpublished(UUID recipeId, String authorUsername, String title, String reason) {

    }

    /**
     * Gửi thông báo cho admin khi có báo cáo mới
     */
    @Override
    public void notifyAdminsNewReport(Report report, String reporterUsername) {
        try {
            // Lấy danh sách admin usernames
            List<String> adminUsernames = queryRepository.findAdminUsernames();

            if (adminUsernames.isEmpty()) {
                log.warn("No active admins to notify about report {}", report.getReportId());
                return;
            }

            // Xác định target của báo cáo
            String targetType;
            String targetName;

            if (report.getReportedId() != null) {
                targetType = "USER";
                targetName = queryRepository.findUsernameById(report.getReportedId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            } else if (report.getRecipeId() != null) {
                targetType = "RECIPE";
                targetName = queryRepository.findRecipeTitleById(report.getRecipeId())
                        .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));
            } else {
                targetType = "UNKNOWN";
                targetName = "Unknown";
            }

            // Tạo notification message
            NewReportNotification notification = NewReportNotification.builder()
                    .reportId(report.getReportId())
                    .reporterUsername(reporterUsername)
                    .reportType(report.getReportType())
                    .targetType(targetType)
                    .targetName(targetName)
                    .createdAt(report.getCreatedAt())
                    .build();

            NotificationMessage message = NotificationMessage.builder()
                    .type("NEW_REPORT")
                    .title("Báo cáo mới")
                    .message(String.format("Người dùng @%s đã báo cáo %s '%s' vì %s",
                            reporterUsername,
                            targetType.equals("USER") ? "người dùng" : "công thức",
                            targetName,
                            getReportTypeVietnamese(report.getReportType())))
                    .data(Map.of(
                            "reportId", report.getReportId().toString(),
                            "reportType", report.getReportType().toString(),
                            "targetType", targetType,
                            "targetName", targetName,
                            "reporterUsername", reporterUsername
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();

            // Gửi thông báo cho tất cả admin qua WebSocket
            adminUsernames.forEach(adminUsername -> {
                try {
                    webSocketHandler.sendToUser(adminUsername, message);
                    log.info("Notified admin {} about new report {}", adminUsername, report.getReportId());
                } catch (Exception e) {
                    log.error("Failed to notify admin {} via WebSocket", adminUsername, e);
                }
            });

            log.info("Successfully notified {} admins about report {}",
                    adminUsernames.size(), report.getReportId());

        } catch (Exception e) {
            log.error("Failed to notify admins about report {}", report.getReportId(), e);
        }
    }

    /**
     * Gửi thông báo cho user khi báo cáo của họ được xử lý
     */
    @Override
    public void notifyReporterReviewComplete(Report report, String reporterUsername, String reviewerUsername) {
        try {
            String statusText = report.getStatus() == ReportStatus.APPROVED ? "được chấp nhận" : "bị từ chối";

            NotificationMessage message = NotificationMessage.builder()
                    .type("REPORT_REVIEWED")
                    .title("Báo cáo " + (report.getStatus() == ReportStatus.APPROVED ? "được chấp nhận" : "bị từ chối"))
                    .message(String.format("Báo cáo của bạn đã %s bởi quản trị viên @%s",
                            statusText, reviewerUsername))
                    .data(Map.of(
                            "reportId", report.getReportId().toString(),
                            "status", report.getStatus().toString(),
                            "reviewerUsername", reviewerUsername,
                            "adminNote", report.getAdminNote() != null ? report.getAdminNote() : ""
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();

            webSocketHandler.sendToUser(reporterUsername, message);

            log.info("Đã thông báo cho người báo cáo {} về kết quả xem xét báo cáo {}",
                    reporterUsername, report.getReportId());

        } catch (Exception e) {
            log.error("Không thể thông báo cho người báo cáo về kết quả xem xét", e);
        }
    }

    /**
     * Gửi thông báo cho user bị báo cáo khi report được approved
     */
    @Override
    public void notifyReportedUser(UUID userId, String username, ReportType reportType, String action) {
        try {
            NotificationMessage message = NotificationMessage.builder()
                    .type("USER_WARNING")
                    .title("Cảnh báo tài khoản")
                    .message(String.format("Bạn đã nhận được cảnh báo vì vi phạm %s. Hành động: %s",
                            getReportTypeVietnamese(reportType), action))
                    .data(Map.of(
                            "reportType", reportType.toString(),
                            "action", action
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();

            webSocketHandler.sendToUser(username, message);

            log.info("Đã thông báo cảnh báo vi phạm cho người dùng {}", username);

        } catch (Exception e) {
            log.error("Không thể thông báo cho người dùng bị báo cáo", e);
        }
    }

    /**
     * Gửi thông báo cho recipe author khi recipe bị unpublish do báo cáo
     */
    @Override
    public void notifyRecipeAuthorUnpublished(UUID recipeId, String authorUsername, String recipeTitle) {
        try {
            NotificationMessage message = NotificationMessage.builder()
                    .type("RECIPE_UNPUBLISHED")
                    .title("Công thức đã bị hủy xuất bản")
                    .message(String.format("Công thức '%s' của bạn đã bị hủy xuất bản do vi phạm chính sách",
                            recipeTitle))
                    .data(Map.of(
                            "recipeId", recipeId.toString(),
                            "recipeTitle", recipeTitle,
                            "action", "unpublished"
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();

            webSocketHandler.sendToUser(authorUsername, message);

            log.info("Đã thông báo cho tác giả công thức {} về việc hủy xuất bản công thức {}",
                    authorUsername, recipeId);

        } catch (Exception e) {
            log.error("Không thể thông báo cho tác giả công thức về việc hủy xuất bản", e);
        }
    }

    /**
     * Thông báo auto-moderation cho user
     */
    @Override
    public void notifyAutoDisableUser(UUID userId, String username, long reportCount) {
        try {
            NotificationMessage message = NotificationMessage.builder()
                    .type("ACCOUNT_SUSPENDED")
                    .title("Tài khoản bị tạm khóa")
                    .message(String.format("Tài khoản của bạn đã bị tạm thời khóa do có %d báo cáo đang chờ xử lý. Vui lòng liên hệ bộ phận hỗ trợ.",
                            reportCount))
                    .data(Map.of(
                            "reportCount", reportCount,
                            "action", "auto_suspended"
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();

            webSocketHandler.sendToUser(username, message);

            log.info("Đã thông báo cho người dùng {} về việc tự động tạm khóa", username);

        } catch (Exception e) {
            log.error("Không thể thông báo cho người dùng về việc tự động tạm khóa", e);
        }
    }

    /**
     * Thông báo auto-moderation cho recipe author
     */
    @Override
    public void notifyAutoUnpublishRecipe(UUID recipeId, String authorUsername, String recipeTitle, long reportCount) {
        try {
            NotificationMessage message = NotificationMessage.builder()
                    .type("RECIPE_AUTO_UNPUBLISHED")
                    .title("Công thức tự động bị hủy xuất bản")
                    .message(String.format("Công thức '%s' của bạn đã tự động bị hủy xuất bản do có %d báo cáo đang chờ xử lý",
                            recipeTitle, reportCount))
                    .data(Map.of(
                            "recipeId", recipeId.toString(),
                            "recipeTitle", recipeTitle,
                            "reportCount", reportCount,
                            "action", "auto_unpublished"
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();

            webSocketHandler.sendToUser(authorUsername, message);

            log.info("Đã thông báo cho tác giả công thức {} về việc tự động hủy xuất bản công thức {}",
                    authorUsername, recipeId);

        } catch (Exception e) {
            log.error("Không thể thông báo cho tác giả công thức về việc tự động hủy xuất bản", e);
        }
    }

    /**
     * Broadcast pending report count update cho tất cả admin
     */
    @Override
    public void broadcastPendingCountUpdate(long pendingCount) {
        try {
            List<String> adminUsernames = queryRepository.findAdminUsernames();

            NotificationMessage message = NotificationMessage.builder()
                    .type("PENDING_REPORTS_UPDATE")
                    .title("Báo cáo chờ xử lý")
                    .message(String.format("%d báo cáo đang chờ xem xét", pendingCount))
                    .data(Map.of("pendingCount", pendingCount))
                    .timestamp(LocalDateTime.now())
                    .build();

            adminUsernames.forEach(adminUsername -> {
                try {
                    webSocketHandler.sendToUser(adminUsername, message);
                } catch (Exception e) {
                    log.error("Không thể gửi cập nhật số lượng báo cáo chờ xử lý cho admin {}", adminUsername, e);
                }
            });

        } catch (Exception e) {
            log.error("Không thể gửi cập nhật số lượng báo cáo chờ xử lý", e);
        }
    }

    private String getReportTypeVietnamese(ReportType reportType) {
        return switch (reportType) {
            case SPAM -> "spam";
            case INAPPROPRIATE -> "nội dung không phù hợp";
            case COPYRIGHT -> "vi phạm bản quyền";
            case HARASSMENT -> "quấy rối";
            case FAKE -> "thông tin giả mạo";
            case MISLEADING -> "gây hiểu lầm";
            case OTHER -> "vi phạm khác";
        };
    }

    private String buildReviewCompleteMessage(Report report) {
        StringBuilder message = new StringBuilder();

        if (report.getStatus() == ReportStatus.APPROVED) {
            message.append("Báo cáo của bạn đã được phê duyệt. ");

            if (report.getActionTaken() != null) {
                message.append("Hành động đã thực hiện: ")
                        .append(report.getActionTaken().getDisplayName())
                        .append(". ");
            }

            if (report.getActionDescription() != null) {
                message.append(report.getActionDescription());
            }
        } else if (report.getStatus() == ReportStatus.REJECTED) {
            message.append("Báo cáo của bạn đã được xem xét nhưng không đủ cơ sở để xử lý. ");

            if (report.getActionDescription() != null) {
                message.append(report.getActionDescription());
            }
        }

        return message.toString();
    }
}
