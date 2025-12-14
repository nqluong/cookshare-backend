package com.backend.cookshare.system.repository.projection;

import com.backend.cookshare.authentication.enums.UserRole;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projection đầy đủ cho Report với tất cả thông tin liên quan (1 query duy nhất)
 */
public interface ReportProjection {
    // Report basic info
    UUID getReportId();
    UUID getReporterId();
    UUID getReportedId();
    UUID getRecipeId();
    ReportType getReportType();
    String getReason();
    String getDescription();
    ReportStatus getStatus();
    ReportActionType getActionTaken();
    String getActionDescription();
    String getAdminNote();
    UUID getReviewedBy();
    LocalDateTime getReviewedAt();
    LocalDateTime getCreatedAt();
    Boolean getReportersNotified();
    
    // Reporter info
    UUID getReporterUserId();
    String getReporterUsername();
    String getReporterFullName();
    String getReporterAvatarUrl();
    
    // Reported user info
    UUID getReportedUserId();
    String getReportedUsername();
    String getReportedEmail();
    String getReportedAvatarUrl();
    String getReportedRole();
    Boolean getReportedIsActive();
    
    // Reported recipe info
    UUID getReportedRecipeId();
    String getReportedRecipeTitle();
    String getReportedRecipeSlug();
    String getReportedRecipeFeaturedImage();
    String getReportedRecipeStatus();
    Boolean getReportedRecipeIsPublished();
    Integer getReportedRecipeViewCount();
    UUID getReportedRecipeUserId();
    String getReportedRecipeAuthorUsername();
    
    // Reviewer info
    UUID getReviewerUserId();
    String getReviewerUsername();
    String getReviewerFullName();
    String getReviewerAvatarUrl();
}
