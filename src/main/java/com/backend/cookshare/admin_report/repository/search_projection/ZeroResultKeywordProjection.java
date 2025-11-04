package com.backend.cookshare.admin_report.repository.search_projection;

import java.time.LocalDateTime;

public interface ZeroResultKeywordProjection {
    String getSearchQuery();
    Long getSearchCount();
    Long getUniqueUsers();
    LocalDateTime getFirstSearched();
    LocalDateTime getLastSearched();
}
