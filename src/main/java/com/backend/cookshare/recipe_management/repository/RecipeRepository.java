package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    // Top rated recipes
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

    // Count top rated recipes for pagination
    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.isPublished = true AND r.ratingCount >= :minRatingCount")
    long countTopRatedRecipes(@Param("minRatingCount") int minRatingCount);
}
