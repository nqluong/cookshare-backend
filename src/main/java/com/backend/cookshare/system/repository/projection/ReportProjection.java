package com.backend.cookshare.system.repository.projection;

import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ReportProjection {
    UUID getReportId();
    UUID getReporterId();
    UUID getReportedId();
    UUID getRecipeId();
    ReportType getReportType();
    String getReason();
    String getDescription();
    ReportStatus getStatus();
    String getAdminNote();
    UUID getReviewedBy();
    LocalDateTime getReviewedAt();
    LocalDateTime getCreatedAt();
}
