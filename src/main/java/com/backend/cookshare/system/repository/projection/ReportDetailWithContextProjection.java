package com.backend.cookshare.system.repository.projection;

import java.util.UUID;

public interface ReportDetailWithContextProjection {
    UUID getReportId();
    UUID getReporterId();
    String getReporterFullName();
    String getReporterUsername();
    String getReportType();
    String getReason();
    String getDescription();
    java.time.LocalDateTime getCreatedAt();
    String getRecipeTitle();
    String getRecipeFeaturedImage();
    UUID getAuthorId();
    String getAuthorUsername();
    String getAuthorFullName();
}
