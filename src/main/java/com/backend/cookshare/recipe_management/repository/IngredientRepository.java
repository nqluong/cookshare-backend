package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.Ingredient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID>, JpaSpecificationExecutor<Ingredient> {
    @Query(value = """
        SELECT i.ingredient_id, i.name, COUNT(DISTINCT ri.recipe_id) as recipe_count
        FROM ingredients i
        INNER JOIN recipe_ingredients ri ON i.ingredient_id = ri.ingredient_id
        INNER JOIN recipes r ON ri.recipe_id = r.recipe_id
        WHERE r.is_published = true
        GROUP BY i.ingredient_id, i.name
        ORDER BY recipe_count DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findTop10MostUsedIngredients();
}

