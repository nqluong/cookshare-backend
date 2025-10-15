package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, UUID> {
    List<RecipeIngredient> findByRecipeId(UUID recipeId);
}
