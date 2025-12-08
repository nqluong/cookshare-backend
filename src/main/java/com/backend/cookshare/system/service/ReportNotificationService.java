package com.backend.cookshare.system.service;

import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportType;

import java.util.UUID;

public interface ReportNotificationService {
    void notifyAdminsNewReport(Report report, String reporterUsername);

    void notifyReporterReviewComplete(Report report, String reporterUsername, String reviewerUsername);

    void notifyReportedUser(UUID userId, String username, ReportType reportType, String action);

    void notifyRecipeAuthorUnpublished(UUID recipeId, String authorUsername, String recipeTitle);

    void notifyAutoDisableUser(UUID userId, String username, long reportCount);

    void notifyAutoUnpublishRecipe(UUID recipeId, String authorUsername, String recipeTitle, long reportCount);

    void broadcastPendingCountUpdate(long pendingCount);

    /**
     * Thông báo cho reporter về kết quả xử lý
     */
    void notifyReporterReviewComplete(Report report, String reporterUsername, UUID reporterId);

    /**
     * Thông báo cho người bị report
     */
    void notifyReportedUser(UUID userId, String username, String actionDescription);

    /**
     * Thông báo cho tác giả công thức khi bị unpublish
     */
    void notifyRecipeAuthorUnpublished(UUID recipeId, String authorUsername, String title, String reason);
}
