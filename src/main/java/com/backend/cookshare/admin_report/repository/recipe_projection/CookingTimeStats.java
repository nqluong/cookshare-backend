package com.backend.cookshare.admin_report.repository.recipe_projection;

public interface CookingTimeStats {
    Double getAvgCookTime();
    Double getAvgPrepTime();
    Double getAvgTotalTime();
}
