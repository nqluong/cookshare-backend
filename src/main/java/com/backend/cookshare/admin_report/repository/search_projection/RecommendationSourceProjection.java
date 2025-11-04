package com.backend.cookshare.admin_report.repository.search_projection;

public interface RecommendationSourceProjection {
    String getSource();
    Long getShown();
    Long getClicked();
}
