package com.backend.cookshare.system.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ReportDetailWithContextProjection {
    // Report basic info
    UUID getReportId();
    UUID getReporterId();
    String getReporterFullName();
    String getReporterUsername();
    String getReporterAvatarUrl();
    String getReportType();
    String getReason();
    String getDescription();
    LocalDateTime getCreatedAt();
    
    // Report status & review info
    String getStatus();
    String getActionTaken();
    String getActionDescription();
    String getAdminNote();
    UUID getReviewedBy();
    LocalDateTime getReviewedAt();
    
    // Recipe info
    String getRecipeTitle();
    String getRecipeFeaturedImage();
    
    // Author info
    UUID getAuthorId();
    String getAuthorUsername();
    String getAuthorFullName();
    String getAuthorAvatarUrl();
    
    // Reviewer info
    String getReviewerUsername();
    String getReviewerFullName();
}
