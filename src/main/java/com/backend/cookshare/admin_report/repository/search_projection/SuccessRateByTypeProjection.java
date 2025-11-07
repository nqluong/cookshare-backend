package com.backend.cookshare.admin_report.repository.search_projection;

public interface SuccessRateByTypeProjection {
    String getSearchType();
    Long getTotalSearches();
    Long getSuccessfulSearches();
}
