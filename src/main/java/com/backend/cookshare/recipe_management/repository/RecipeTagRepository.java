package com.backend.cookshare.recipe_management.repository;


import com.backend.cookshare.recipe_management.dto.response.TagResponse;
import com.backend.cookshare.recipe_management.entity.RecipeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeTagRepository extends JpaRepository<RecipeTag, UUID> {
    @Query(value = """
        SELECT new com.backend.cookshare.recipe_management.dto.response.TagResponse(
            t.tagId,
            t.name,
            t.slug,
            t.color,
            t.usageCount,
            t.isTrending,
            t.createdAt
        )
        FROM RecipeTag rt
        JOIN Tag t ON rt.tagId = t.tagId
        WHERE rt.recipeId = :recipeId
        ORDER BY t.name ASC
        """)
    List<TagResponse> findTagsByRecipeId(@Param("recipeId") UUID recipeId);


    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO recipe_tags (recipe_id, tag_id)
        VALUES (:recipeId, :tagId)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    void insertRecipeTag(@Param("recipeId") UUID recipeId, @Param("tagId") UUID tagId);


}
