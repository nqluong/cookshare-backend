package com.backend.cookshare.interaction.repository;

import com.backend.cookshare.interaction.entity.RecipeRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRatingRespository extends JpaRepository<RecipeRating, UUID> {
    @Query("SELECT AVG(r.rating) FROM RecipeRating r WHERE r.recipeId = :recipeId")
    BigDecimal getAverageRatingByRecipeId(@Param("recipeId") UUID recipeId);
    Boolean existsByUserIdAndRecipeId(UUID userId, UUID recipeId);
    Optional<RecipeRating> findByUserIdAndRecipeId(UUID userId, UUID recipeId);
}
