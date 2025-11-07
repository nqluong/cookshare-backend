package com.backend.cookshare.admin_report.repository.recipe_projection;

public interface EngagementRateProjection {
    String getRecipeId();
    String getTitle();
    Long getViewCount();
    Long getEngagementCount();
    Double getEngagementRate();
}
