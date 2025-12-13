package com.backend.cookshare.system.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ReportDetailWithContextProjection {
    UUID getReportId();
    UUID getReporterId();
    String getReporterFullName();
    String getReporterUsername();
    String getReporterAvatar();
    String getReportType();
    String getReason();
    String getDescription();
    LocalDateTime getCreatedAt();
    String getRecipeTitle();
    String getRecipeFeaturedImage();
    UUID getAuthorId();
    String getAuthorUsername();
    String getAuthorFullName();
    String getAuthorAvatar();
}
