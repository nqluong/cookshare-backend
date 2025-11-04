package com.backend.cookshare.admin_report.repository;

import java.sql.Date;

public interface TimeSeriesProjection {
    Date getDate();
    Long getRecipeCount();
    Long getTotalViews();
    Long getTotalLikes();
}
