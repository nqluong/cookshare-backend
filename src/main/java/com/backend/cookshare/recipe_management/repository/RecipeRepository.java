package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID>, JpaSpecificationExecutor<Recipe> {
    // Featured recipes
    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true AND r.isFeatured = true ORDER BY r.updatedAt DESC")
    Page<Recipe> findFeaturedRecipes(Pageable pageable);

    // Popular recipes - using custom scoring
    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true ORDER BY " +
            "(r.likeCount * 2.0 + r.viewCount * 0.5 + r.saveCount * 1.5) DESC")
    Page<Recipe> findPopularRecipes(Pageable pageable);
    // Newest recipes
    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true ORDER BY r.createdAt DESC")
    Page<Recipe> findNewestRecipes(Pageable pageable);

    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true AND r.ratingCount >= :minRatingCount " +
            "ORDER BY r.averageRating DESC, r.ratingCount DESC")
    Page<Recipe> findTopRatedRecipes(@Param("minRatingCount") int minRatingCount, Pageable pageable);

    // Trending recipes - using custom scoring
    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true ORDER BY " +
            "(r.viewCount + r.likeCount * 3.0 + r.ratingCount * 2.0) DESC, r.createdAt DESC")
    Page<Recipe> findTrendingRecipes(Pageable pageable);

    // Get recipes by user IDs for batch user name lookup
    @Query("SELECT r FROM Recipe r WHERE r.userId IN :userIds")
    List<Recipe> findByUserIdIn(@Param("userIds") List<UUID> userIds);

    // Count total published recipes for pagination
    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.isPublished = true")
    long countPublishedRecipes();

    // Count featured recipes for pagination
    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.isPublished = true AND r.isFeatured = true")
    long countFeaturedRecipes();

    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.isPublished = true AND r.ratingCount >= :minRatingCount")
    long countTopRatedRecipes(@Param("minRatingCount") int minRatingCount);
    List<Recipe> findByUserId(UUID userId);

    UUID findUserIdByRecipeId(UUID recipeId);
    //Tong so luot thich cua tat ca cac cong thuc
    @Query("SELECT COALESCE(SUM(r.likeCount), 0) FROM Recipe r WHERE r.userId = :userId")
    Integer getTotalLikeCountByUserId(@Param("userId") UUID userId);

    @Query(value = """
SELECT
    s.step_number,
    s.instruction,
    s.image_url,
    s.video_url,
    s.estimated_time,
    s.tips,

    i.ingredient_id,
    i.name AS ingredient_name,
    i.slug AS ingredient_slug,
    i.description AS ingredient_description,
    ri.quantity,
    ri.unit,
    ri.notes AS ingredient_notes,
    ri.order_index,

    t.tag_id,
    t.name AS tag_name,
    t.slug AS tag_slug,
    t.color AS tag_color,
    t.usage_count,
    t.is_trending,
    t.created_at AS tag_created_at,

    c.category_id,
    c.name AS category_name,
    c.slug AS category_slug,
    c.description AS category_description,
    c.icon_url AS category_icon,
    c.parent_id,
    c.is_active,
    c.created_at AS category_created_at

FROM recipe_steps s
LEFT JOIN recipe_ingredients ri ON s.recipe_id = ri.recipe_id
LEFT JOIN ingredients i ON ri.ingredient_id = i.ingredient_id
LEFT JOIN recipe_tags rt ON s.recipe_id = rt.recipe_id
LEFT JOIN tags t ON rt.tag_id = t.tag_id
LEFT JOIN recipe_categories rc ON s.recipe_id = rc.recipe_id
LEFT JOIN categories c ON rc.category_id = c.category_id
WHERE s.recipe_id = :recipeId
""", nativeQuery = true)
    List<Object[]> findRecipeDetailsById(@Param("recipeId") UUID recipeId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO recipe_tags (recipe_id, tag_id)
        VALUES (:recipeId, :tagId)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    void insertRecipeTag(@Param("recipeId") UUID recipeId, @Param("tagId") UUID tagId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO recipe_categories (recipe_id, category_id)
        VALUES (:recipeId, :categoryId)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    void insertRecipeCategory(@Param("recipeId") UUID recipeId, @Param("categoryId") UUID categoryId);

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
    @Query(value = """
    INSERT INTO recipe_steps (recipe_id, step_number, instruction, image_url, video_url, estimated_time, tips)
    VALUES (:recipeId, :stepNumber, :instruction, :imageUrl, :videoUrl, :estimatedTime, :tips)
    ON CONFLICT DO NOTHING
    """, nativeQuery = true)
    void insertRecipeStep(
            @Param("recipeId") UUID recipeId,
            @Param("stepNumber") Integer stepNumber,
            @Param("instruction") String instruction,
            @Param("imageUrl") String imageUrl,
            @Param("videoUrl") String videoUrl,
            @Param("estimatedTime") Integer estimatedTime,
            @Param("tips") String tips
    );


}
