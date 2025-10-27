package com.backend.cookshare.recipe_management.repository;


import com.backend.cookshare.recipe_management.dto.response.CategoryResponse;
import com.backend.cookshare.recipe_management.entity.RecipeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeCategoryRepository extends JpaRepository<RecipeCategory, UUID> {
    @Query(value = """
        SELECT new com.backend.cookshare.recipe_management.dto.response.CategoryResponse(
            c.categoryId,
            c.name,
            c.slug,
            c.description,
            c.iconUrl,
            c.parentId,
            c.isActive,
            c.createdAt
        )
        FROM RecipeCategory rc
        JOIN Category c ON rc.categoryId = c.categoryId
        WHERE rc.recipeId = :recipeId
        ORDER BY c.name ASC
        """)
    List<CategoryResponse> findCategoriesByRecipeId(@Param("recipeId") UUID recipeId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO recipe_categories (recipe_id, category_id)
            VALUES (:recipeId, :categoryId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertRecipeCategory(@Param("recipeId") UUID recipeId, @Param("categoryId") UUID categoryId);

}
