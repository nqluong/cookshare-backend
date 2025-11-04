package com.backend.cookshare.admin_report.repository;

public interface RecipeWithCommentProjection extends RecipeProjection{
    Long getCommentCount();
}
