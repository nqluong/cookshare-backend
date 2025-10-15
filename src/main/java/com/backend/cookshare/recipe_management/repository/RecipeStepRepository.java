package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.RecipeStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeStepRepository extends JpaRepository<RecipeStep, UUID> {
    List<RecipeStep> findByRecipeId(UUID recipeId);
}
