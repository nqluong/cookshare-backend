package com.backend.cookshare.admin_report.repository.recipe_projection;

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
