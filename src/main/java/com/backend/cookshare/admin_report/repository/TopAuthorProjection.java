package com.backend.cookshare.admin_report.repository;

public interface TopAuthorProjection {
    String getUserId();
    String getAuthorName();
    String getUsername();
    Long getRecipeCount();
    Long getTotalViews();
    Long getTotalLikes();
    Double getAvgRating();
}
