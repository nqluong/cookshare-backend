package com.backend.cookshare.admin_report.repository.search_projection;

import java.util.UUID;

public interface IngredientRecommendationProjection {
    UUID getIngredientId();
    String getIngredientName();
    Long getSearchCount();
    Long getShown();
    Long getClicked();
}
