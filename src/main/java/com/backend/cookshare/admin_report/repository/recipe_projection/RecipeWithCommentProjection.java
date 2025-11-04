package com.backend.cookshare.admin_report.repository.recipe_projection;

public interface RecipeWithCommentProjection extends RecipeProjection {
    Long getCommentCount();
}
