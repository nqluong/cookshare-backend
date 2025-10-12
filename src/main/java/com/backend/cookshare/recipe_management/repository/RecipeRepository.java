package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository thao tác với bảng recipes
 */
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
}
