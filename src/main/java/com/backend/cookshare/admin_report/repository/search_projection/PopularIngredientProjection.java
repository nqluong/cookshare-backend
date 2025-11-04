package com.backend.cookshare.admin_report.repository.search_projection;

import java.util.UUID;

public interface PopularIngredientProjection {
    UUID getIngredientId();
    String getIngredientName();
    Long getSearchCount();
    Long getDirectSearches();
    Long getRecipeCount();
}
