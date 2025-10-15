package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.RecipeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeCategoryRepository extends JpaRepository<RecipeCategory, UUID> {
    List<RecipeCategory> findByRecipeId(UUID recipeId);
}
