package com.backend.cookshare.admin_report.repository.search_projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PopularKeywordProjection {
    String getSearchQuery();
    Long getSearchCount();
    Long getUniqueUsers();
    BigDecimal getAvgResults();
    LocalDateTime getLastSearched();
}
