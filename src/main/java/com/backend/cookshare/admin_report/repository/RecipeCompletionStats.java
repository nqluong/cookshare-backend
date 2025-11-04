package com.backend.cookshare.admin_report.repository;

public interface RecipeCompletionStats {
    Long getTotalRecipes();
    Long getWithDescription();
    Long getWithImage();
    Long getWithVideo();
    Long getWithIngredients();
    Long getWithSteps();
    Long getCompleteRecipes();
    Double getCompletionRate();
}
