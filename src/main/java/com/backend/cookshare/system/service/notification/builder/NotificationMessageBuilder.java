package com.backend.cookshare.system.service.notification.builder;

import com.backend.cookshare.system.dto.request.NotificationMessage;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationMessageBuilder {
    public NotificationMessage buildNewReportMessage(
            Report report,
            String reporterUsername,
            String targetType,
            String targetName) {

        return NotificationMessage.builder()
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
    }

    public NotificationMessage buildReportReviewedMessage(
            Report report,
            String reviewerUsername) {

        String statusText = report.getStatus() == ReportStatus.RESOLVED ?
                "được chấp nhận" : "bị từ chối";

        return NotificationMessage.builder()
                .type("REPORT_REVIEWED")
                .title("Báo cáo " + statusText)
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
    }

    public NotificationMessage buildUserWarningMessage(
            ReportType reportType,
            String action) {

        return NotificationMessage.builder()
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
    }

    public NotificationMessage buildUserSuspendedMessage(int days) {
        return NotificationMessage.builder()
                .type("ACCOUNT_SUSPENDED")
                .title("Tài khoản bị tạm khóa")
                .message(String.format("Tài khoản của bạn đã bị tạm khóa %d ngày do vi phạm chính sách. " +
                        "Vui lòng liên hệ bộ phận hỗ trợ.", days))
                .data(Map.of(
                        "suspensionDays", days,
                        "action", "suspended"
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public NotificationMessage buildUserBannedMessage() {
        return NotificationMessage.builder()
                .type("ACCOUNT_BANNED")
                .title("Tài khoản bị khóa vĩnh viễn")
                .message("Tài khoản của bạn đã bị khóa vĩnh viễn do vi phạm nghiêm trọng chính sách. " +
                        "Vui lòng liên hệ bộ phận hỗ trợ nếu bạn cho rằng đây là nhầm lẫn.")
                .data(Map.of("action", "banned"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public NotificationMessage buildRecipeUnpublishedMessage(
            UUID recipeId,
            String recipeTitle,
            String reason) {

        return NotificationMessage.builder()
                .type("RECIPE_UNPUBLISHED")
                .title("Công thức đã bị hủy xuất bản")
                .message(String.format("Công thức '%s' của bạn đã bị hủy xuất bản. Lý do: %s",
                        recipeTitle, reason))
                .data(Map.of(
                        "recipeId", recipeId.toString(),
                        "recipeTitle", recipeTitle,
                        "reason", reason,
                        "action", "unpublished"
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public NotificationMessage buildRecipeEditRequiredMessage(
            UUID recipeId,
            String recipeTitle,
            String editRequirement) {

        return NotificationMessage.builder()
                .type("RECIPE_EDIT_REQUIRED")
                .title("Yêu cầu chỉnh sửa công thức")
                .message(String.format("Công thức '%s' của bạn cần được chỉnh sửa. Yêu cầu: %s",
                        recipeTitle, editRequirement))
                .data(Map.of(
                        "recipeId", recipeId.toString(),
                        "recipeTitle", recipeTitle,
                        "editRequirement", editRequirement,
                        "action", "edit_required"
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public NotificationMessage buildAutoDisableUserMessage(long reportCount) {
        return NotificationMessage.builder()
                .type("ACCOUNT_AUTO_SUSPENDED")
                .title("Tài khoản bị tạm khóa tự động")
                .message(String.format("Tài khoản của bạn đã bị tạm thời khóa do có %d báo cáo đang chờ xử lý. " +
                        "Vui lòng liên hệ bộ phận hỗ trợ.", reportCount))
                .data(Map.of(
                        "reportCount", reportCount,
                        "action", "auto_suspended"
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public NotificationMessage buildAutoUnpublishRecipeMessage(
            UUID recipeId,
            String recipeTitle,
            long reportCount) {

        return NotificationMessage.builder()
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
    }

    public NotificationMessage buildPendingCountUpdateMessage(long pendingCount) {
        return NotificationMessage.builder()
                .type("PENDING_REPORTS_UPDATE")
                .title("Báo cáo chờ xử lý")
                .message(String.format("%d báo cáo đang chờ xem xét", pendingCount))
                .data(Map.of("pendingCount", pendingCount))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public NotificationMessage buildAutoContentRemovedMessage(
            UUID recipeId,
            String recipeTitle,
            long reportCount,
            ReportType reportType) {

        return NotificationMessage.builder()
                .type("RECIPE_AUTO_REMOVED")
                .title("Nội dung tự động bị xóa")
                .message(String.format("Công thức '%s' của bạn đã tự động bị xóa do có %d báo cáo về %s",
                        recipeTitle, reportCount, getReportTypeVietnamese(reportType)))
                .data(Map.of(
                        "recipeId", recipeId.toString(),
                        "recipeTitle", recipeTitle,
                        "reportCount", reportCount,
                        "reportType", reportType.toString(),
                        "action", "auto_removed"
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public NotificationMessage buildAutoRecipeEditRequiredMessage(
            UUID recipeId,
            String recipeTitle,
            long reportCount,
            ReportType reportType) {

        return NotificationMessage.builder()
                .type("RECIPE_AUTO_EDIT_REQUIRED")
                .title("Yêu cầu chỉnh sửa công thức")
                .message(String.format("Công thức '%s' của bạn cần chỉnh sửa do có %d báo cáo về %s. " +
                        "Vui lòng xem xét và cập nhật nội dung.",
                        recipeTitle, reportCount, getReportTypeVietnamese(reportType)))
                .data(Map.of(
                        "recipeId", recipeId.toString(),
                        "recipeTitle", recipeTitle,
                        "reportCount", reportCount,
                        "reportType", reportType.toString(),
                        "action", "auto_edit_required"
                ))
                .timestamp(LocalDateTime.now())
                .build();
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
}
