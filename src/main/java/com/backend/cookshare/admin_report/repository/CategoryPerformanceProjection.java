package com.backend.cookshare.admin_report.repository;

public interface CategoryPerformanceProjection {
    String getCategoryName();
    Long getRecipeCount();
    Long getTotalViews();
    Long getTotalLikes();
    Double getAvgRating();
    Double getAvgEngagementRate();
}
