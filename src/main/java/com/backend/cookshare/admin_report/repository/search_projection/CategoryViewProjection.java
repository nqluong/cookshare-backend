package com.backend.cookshare.admin_report.repository.search_projection;

import java.util.UUID;

public interface CategoryViewProjection {
    UUID getCategoryId();
    String getCategoryName();
    Long getViewCount();
    Long getUniqueUsers();
    Long getRecipeCount();
}
