package com.backend.cookshare.recipe_management.repository;


import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.entity.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, UUID> {
    @Query(value = """
            SELECT new com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse(
                i.ingredientId,
                i.name,
                i.slug,
                i.description,
                i.category,
            
                ri.quantity,
                ri.unit,
                ri.notes,
                ri.orderIndex,
                i.createdAt
            )
            FROM RecipeIngredient ri
            JOIN Ingredient i ON ri.ingredientId = i.ingredientId
            WHERE ri.recipeId = :recipeId
            ORDER BY ri.orderIndex ASC
            """)
    List<RecipeIngredientResponse> findIngredientsByRecipeId(@Param("recipeId") UUID recipeId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO recipe_ingredients (recipe_id, ingredient_id)
            VALUES (:recipeId, :ingredientId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertRecipeIngredient(@Param("recipeId") UUID recipeId, @Param("ingredientId") UUID ingredientId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM recipe_ingredients WHERE recipe_id = :recipeId", nativeQuery = true)
    void deleteAllByRecipeId(@Param("recipeId") UUID recipeId);
}
