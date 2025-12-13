package com.backend.cookshare.system.service.action;

import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.service.ReportNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportActionExecutor {
    private final ReportQueryRepository reportQueryRepository;
    private final ReportNotificationService notificationService;

    public void execute(Report report) {
        if (report.getActionTaken() == null) {
            return;
        }

        switch (report.getActionTaken()) {
            case NO_ACTION -> handleNoAction(report);
            case USER_WARNED -> handleUserWarned(report);
            case USER_SUSPENDED -> handleUserSuspended(report);
            case USER_BANNED -> handleUserBanned(report);
            case RECIPE_UNPUBLISHED -> handleRecipeUnpublished(report);
            case RECIPE_EDITED -> handleRecipeEdited(report);
            case CONTENT_REMOVED -> handleContentRemoved(report);
            case OTHER -> handleOtherAction(report);
        }

        // Thông báo cho admins về việc hoàn thành xử lý
        notificationService.notifyAdminsActionCompleted(report);
    }

    /**
     * Lấy ID tác giả từ công thức.
     * Báo cáo chỉ áp dụng cho công thức, nên cần lấy tác giả từ công thức để thực hiện hành động lên user.
     */
    private UUID getRecipeAuthorId(Report report) {
        if (report.getRecipeId() == null) {
            return null;
        }
        return reportQueryRepository.findAuthorIdByRecipeId(report.getRecipeId()).orElse(null);
    }

    private void handleNoAction(Report report) {
        log.info("Báo cáo {} - Không có hành động nào được thực hiện (đã từ chối)", report.getReportId());
    }

    private void handleUserWarned(Report report) {
        UUID authorId = getRecipeAuthorId(report);
        if (authorId == null) {
            log.warn("Không thể cảnh báo người dùng: không tìm thấy tác giả công thức cho báo cáo {}", report.getReportId());
            return;
        }

        // Gửi thông báo cảnh báo
        notificationService.notifyUserWarned(report, authorId);

        log.info("Tác giả {} đã được cảnh báo vì công thức {} bị báo cáo {}", authorId, report.getRecipeId(), report.getReportType());
    }

    private void handleUserSuspended(Report report) {
        UUID authorId = getRecipeAuthorId(report);
        if (authorId == null) {
            log.warn("Không thể tạm khóa người dùng: không tìm thấy tác giả công thức cho báo cáo {}", report.getReportId());
            return;
        }

        int suspensionDays = 30;
        reportQueryRepository.suspendUser(authorId, suspensionDays);

        // Gửi thông báo tạm khóa
        notificationService.notifyUserSuspended(report, authorId, suspensionDays);

        log.warn("Tác giả {} đã bị tạm khóa trong {} ngày vì công thức {} bị báo cáo", authorId, suspensionDays, report.getRecipeId());
    }

    private void handleUserBanned(Report report) {
        UUID authorId = getRecipeAuthorId(report);
        if (authorId == null) {
            log.warn("Không thể cấm người dùng: không tìm thấy tác giả công thức cho báo cáo {}", report.getReportId());
            return;
        }

        reportQueryRepository.disableUser(authorId);

        // Gửi thông báo cấm vĩnh viễn
        notificationService.notifyUserBanned(report, authorId);

        log.warn("Tác giả {} đã bị cấm vĩnh viễn vì công thức {} bị báo cáo", authorId, report.getRecipeId());
    }

    private void handleRecipeUnpublished(Report report) {
        UUID recipeId = report.getRecipeId();
        if (recipeId == null) {
            log.warn("Không thể gỡ công thức: recipeId là null cho báo cáo {}", report.getReportId());
            return;
        }

        reportQueryRepository.unpublishRecipe(recipeId);

        // Gửi thông báo cho tác giả công thức
        notificationService.notifyRecipeUnpublished(report, recipeId);

        log.info("Công thức {} đã bị gỡ xuống", recipeId);
    }

    private void handleRecipeEdited(Report report) {
        UUID recipeId = report.getRecipeId();
        if (recipeId == null) {
            log.warn("Không thể gửi yêu cầu chỉnh sửa: recipeId là null cho báo cáo {}", report.getReportId());
            return;
        }

        // Gửi thông báo yêu cầu chỉnh sửa
        notificationService.notifyRecipeEditRequired(report, recipeId);

        log.info("Yêu cầu chỉnh sửa đã được gửi cho công thức {}", recipeId);
    }

    private void handleContentRemoved(Report report) {
        if (report.getRecipeId() != null) {
            // Xóa công thức và gửi thông báo
            reportQueryRepository.unpublishRecipe(report.getRecipeId());
            notificationService.notifyContentRemoved(report, report.getRecipeId());
            log.info("Công thức {} đã bị xóa", report.getRecipeId());
        } else {
            log.info("Nội dung đã bị xóa cho báo cáo {}", report.getReportId());
        }
    }

    private void handleOtherAction(Report report) {
        log.info("Hành động tùy chỉnh cho báo cáo {}: {}",
                report.getReportId(), report.getActionDescription());
    }
}