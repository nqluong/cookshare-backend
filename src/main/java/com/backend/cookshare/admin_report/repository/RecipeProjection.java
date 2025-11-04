package com.backend.cookshare.admin_report.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface RecipeProjection {
    String getRecipeId();
    String getTitle();
    String getSlug();
    Long getViewCount();
    Long getLikeCount();
    Long getSaveCount();
    BigDecimal getAverageRating();
    Long getRatingCount();
    String getAuthorName();
    LocalDateTime getCreatedAt();
}
