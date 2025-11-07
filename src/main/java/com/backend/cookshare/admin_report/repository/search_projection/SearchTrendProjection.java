package com.backend.cookshare.admin_report.repository.search_projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SearchTrendProjection {
    LocalDateTime getPeriod();
    Long getTotalSearches();
    Long getUniqueUsers();
    Long getUniqueQueries();
    Long getSuccessfulSearches();
    BigDecimal getAvgResults();
}
