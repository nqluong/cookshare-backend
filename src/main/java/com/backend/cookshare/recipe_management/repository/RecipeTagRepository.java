package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.RecipeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeTagRepository extends JpaRepository<RecipeTag, UUID> {
    List<RecipeTag> findByRecipe_RecipeId(UUID recipeId);
}
