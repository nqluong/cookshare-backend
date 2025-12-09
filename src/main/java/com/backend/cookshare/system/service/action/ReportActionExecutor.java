package com.backend.cookshare.system.service.action;

import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportActionExecutor {
    private final ReportQueryRepository reportQueryRepository;

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
    }

    private void handleNoAction(Report report) {
        log.info("Báo cáo {} - Không có hành động nào được thực hiện (đã từ chối)", report.getReportId());
    }

    private void handleUserWarned(Report report) {
        if (report.getReportedId() == null) {
            log.warn("Không thể cảnh báo người dùng: reportedId là null cho báo cáo {}", report.getReportId());
            return;
        }

        log.info("Người dùng {} đã được cảnh báo vì {}", report.getReportedId(), report.getReportType());
    }

    private void handleUserSuspended(Report report) {
        if (report.getReportedId() == null) {
            log.warn("Không thể tạm khóa người dùng: reportedId là null cho báo cáo {}", report.getReportId());
            return;
        }

        int suspensionDays = 30;
        reportQueryRepository.suspendUser(report.getReportedId(), suspensionDays);

        log.warn("Người dùng {} đã bị tạm khóa trong {} ngày", report.getReportedId(), suspensionDays);
    }

    private void handleUserBanned(Report report) {
        if (report.getReportedId() == null) {
            log.warn("Không thể cấm người dùng: reportedId là null cho báo cáo {}", report.getReportId());
            return;
        }

        reportQueryRepository.disableUser(report.getReportedId());

        log.warn("Người dùng {} đã bị cấm vĩnh viễn", report.getReportedId());
    }

    private void handleRecipeUnpublished(Report report) {
        if (report.getRecipeId() == null) {
            log.warn("Không thể gỡ công thức: recipeId là null cho báo cáo {}", report.getReportId());
            return;
        }

        reportQueryRepository.unpublishRecipe(report.getRecipeId());

        log.info("Công thức {} đã bị gỡ xuống", report.getRecipeId());
    }

    private void handleRecipeEdited(Report report) {
        if (report.getRecipeId() == null) {
            log.warn("Không thể gửi yêu cầu chỉnh sửa: recipeId là null cho báo cáo {}", report.getReportId());
            return;
        }

        log.info("Yêu cầu chỉnh sửa đã được gửi cho công thức {}", report.getRecipeId());
    }

    private void handleContentRemoved(Report report) {
        log.info("Nội dung đã bị xóa cho báo cáo {}", report.getReportId());
    }

    private void handleOtherAction(Report report) {
        log.info("Hành động tùy chỉnh cho báo cáo {}: {}",
                report.getReportId(), report.getActionDescription());
    }
}