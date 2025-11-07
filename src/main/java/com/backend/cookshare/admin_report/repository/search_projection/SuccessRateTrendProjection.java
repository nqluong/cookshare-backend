package com.backend.cookshare.admin_report.repository.search_projection;

import java.time.LocalDateTime;

public interface SuccessRateTrendProjection {
    LocalDateTime getPeriod();
    Long getTotalSearches();
    Long getSuccessfulSearches();
}
