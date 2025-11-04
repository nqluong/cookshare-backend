package com.backend.cookshare.admin_report.repository.recipe_projection;

import java.sql.Date;

public interface TimeSeriesProjection {
    Date getDate();
    Long getRecipeCount();
    Long getTotalViews();
    Long getTotalLikes();
}
