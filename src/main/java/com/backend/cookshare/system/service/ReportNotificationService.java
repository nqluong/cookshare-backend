package com.backend.cookshare.system.service;

import com.backend.cookshare.system.entity.Report;

import java.util.UUID;

public interface ReportNotificationService {

    /**
     * Thông báo cho admins khi có báo cáo mới
     */
    void notifyAdminsNewReport(Report report, String reporterUsername);

    /**
     * Thông báo cho người báo cáo khi review hoàn tất
     */
    void notifyReporterReviewComplete(Report report, String reporterUsername, UUID reporterId);

    /**
     * Broadcast số lượng báo cáo pending tới admins
     */
    void broadcastPendingCountUpdate(long pendingCount);

    /**
     * Thông báo cho admins khi hoàn thành xử lý action
     */
    void notifyAdminsActionCompleted(Report report);

    /**
     * Thông báo cảnh báo cho người dùng
     */
    void notifyUserWarned(Report report, UUID userId);

    /**
     * Thông báo tạm khóa tài khoản
     */
    void notifyUserSuspended(Report report, UUID userId, int suspensionDays);

    /**
     * Thông báo cấm vĩnh viễn tài khoản
     */
    void notifyUserBanned(Report report, UUID userId);

    /**
     * Thông báo gỡ công thức
     */
    void notifyRecipeUnpublished(Report report, UUID recipeId);

    /**
     * Thông báo yêu cầu chỉnh sửa công thức
     */
    void notifyRecipeEditRequired(Report report, UUID recipeId);

    /**
     * Thông báo nội dung bị xóa
     */
    void notifyContentRemoved(Report report, UUID recipeId);

    /**
     * Thông báo tự động khóa user
     */
    void notifyAutoDisableUser(UUID userId, String username, long reportCount);

    /**
     * Thông báo tự động gỡ recipe
     */
    void notifyAutoUnpublishRecipe(UUID recipeId, UUID authorId, String authorUsername, String recipeTitle, long reportCount);
}
