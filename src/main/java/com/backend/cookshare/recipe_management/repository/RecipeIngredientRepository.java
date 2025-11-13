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
import java.util.Map;
import java.util.UUID;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, UUID> {

    // ✅ METHOD NÀY VẪN CÒN - DÙNG ĐỂ ĐỌC DỮ LIỆU
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

    // ✅ METHOD NÀY ĐÃ SỬA - DÙNG ĐỂ GHI DỮ LIỆU
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO recipe_ingredients (recipe_id, ingredient_id, quantity, unit, notes, order_index)
            VALUES (:recipeId, :ingredientId, :quantity, :unit, :notes, :orderIndex)
            ON CONFLICT (recipe_id, ingredient_id) DO UPDATE
            SET quantity = EXCLUDED.quantity,
                unit = EXCLUDED.unit,
                notes = EXCLUDED.notes,
                order_index = EXCLUDED.order_index
            """, nativeQuery = true)
    void insertRecipeIngredient(
            @Param("recipeId") UUID recipeId,
            @Param("ingredientId") UUID ingredientId,
            @Param("quantity") String quantity,
            @Param("unit") String unit,
            @Param("notes") String notes,
            @Param("orderIndex") Integer orderIndex
    );

    // ✅ METHOD NÀY VẪN CÒN - DÙNG ĐỂ XÓA
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM recipe_ingredients WHERE recipe_id = :recipeId", nativeQuery = true)
    void deleteAllByRecipeId(@Param("recipeId") UUID recipeId);
    @Query(value = "SELECT ingredient_id, quantity, unit, notes FROM recipe_ingredients WHERE recipe_id = :recipeId", nativeQuery = true)
    List<Map<String, Object>> findIngredientDetailsByRecipeId(@Param("recipeId") UUID recipeId);
}