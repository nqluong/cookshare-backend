package com.backend.cookshare.admin_report.repository.search_projection;

import java.math.BigDecimal;

public interface RecommendationStatsProjection {
    Long getShownCount();
    Long getClickedCount();
    BigDecimal getAvgPosition();
}
