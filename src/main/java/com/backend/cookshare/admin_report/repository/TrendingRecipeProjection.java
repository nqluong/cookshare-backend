package com.backend.cookshare.admin_report.repository;

import java.time.LocalDateTime;

public interface TrendingRecipeProjection {
    String getRecipeId();
    String getTitle();
    String getSlug();
    Long getViewCount();
    Long getLikeCount();
    Double getTrendingScore();
    LocalDateTime getCreatedAt();
}
