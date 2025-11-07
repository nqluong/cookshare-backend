package com.backend.cookshare.admin_report.repository.recipe_projection;

public interface MediaStats {
    Long getRecipesWithImage();
    Long getRecipesWithVideo();
    Long getTotalRecipes();
}
