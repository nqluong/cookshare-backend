package com.backend.cookshare.user.repository;

import com.backend.cookshare.user.dto.CollectionRecipeDto;
import com.backend.cookshare.user.entity.CollectionRecipe;
import com.backend.cookshare.user.entity.CollectionRecipeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionRecipeRepository extends JpaRepository<CollectionRecipe, CollectionRecipeId> {

    boolean existsByCollectionIdAndRecipeId(UUID collectionId, UUID recipeId);

    Optional<CollectionRecipe> findByCollectionIdAndRecipeId(UUID collectionId, UUID recipeId);

    @Query("SELECT COUNT(cr) FROM CollectionRecipe cr WHERE cr.collectionId = :collectionId")
    long countByCollectionId(@Param("collectionId") UUID collectionId);

    // ← Native query - Cách mới
    @Query(value = """
        SELECT 
            cr.recipe_id,
            r.title,
            r.slug,
            r.description,
            r.prep_time,
            r.cook_time,
            r.servings,
            r.difficulty,
            r.featured_image,
            r.view_count,
            r.save_count,
            r.like_count,
            CAST(r.average_rating AS varchar)
        FROM collection_recipes cr
        JOIN recipes r ON cr.recipe_id = r.recipe_id
        WHERE cr.collection_id = :collectionId
        ORDER BY cr.added_at DESC
        """, nativeQuery = true)
    Page<Object[]> findRecipesByCollectionId(
            @Param("collectionId") UUID collectionId,
            Pageable pageable
    );

}