package com.backend.cookshare.admin_report.repository.interaction_projection;

import java.util.UUID;

public interface CategoryEngagementProjection {
    UUID getCategoryId();
    String getCategoryName();
    Long getRecipeCount();
    Long getTotalViews();
    Long getTotalLikes();
    Long getTotalComments();
    Long getTotalSaves();
}
