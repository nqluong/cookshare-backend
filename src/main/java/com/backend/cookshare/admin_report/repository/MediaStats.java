package com.backend.cookshare.admin_report.repository;

public interface MediaStats {
    Long getRecipesWithImage();
    Long getRecipesWithVideo();
    Long getTotalRecipes();
}
