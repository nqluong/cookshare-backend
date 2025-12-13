package com.backend.cookshare.system.repository.projection;

import java.util.UUID;

public interface ReportedRecipeInfoProjection {
    UUID getRecipeId();
    String getTitle();
    String getSlug();
    String getFeaturedImage();
    String getStatus();
    Boolean getIsPublished();
    Integer getViewCount();
    UUID getUserId();
    String getAuthorUsername();
    String getAuthorFullName();
}
