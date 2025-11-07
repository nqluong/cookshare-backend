package com.backend.cookshare.admin_report.repository.recipe_projection;

public interface CategoryPerformanceProjection {
    String getCategoryName();
    Long getRecipeCount();
    Long getTotalViews();
    Long getTotalLikes();
    Double getAvgRating();
    Double getAvgEngagementRate();
}
