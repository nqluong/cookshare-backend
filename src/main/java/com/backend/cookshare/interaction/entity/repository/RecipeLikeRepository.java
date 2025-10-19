package com.backend.cookshare.interaction.entity.repository;

import com.backend.cookshare.interaction.entity.RecipeLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecipeLikeRepository extends JpaRepository<RecipeLike, UUID> {
    Boolean existsByUserIdAndRecipeId(UUID userId,UUID recipeId);
    RecipeLike findByUserIdAndRecipeId(UUID userId,UUID recipeId);
}
