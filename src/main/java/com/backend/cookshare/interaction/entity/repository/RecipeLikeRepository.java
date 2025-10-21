package com.backend.cookshare.interaction.entity.repository;

import com.backend.cookshare.interaction.entity.RecipeLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeLikeRepository extends JpaRepository<RecipeLike, UUID> {
    Boolean existsByUserIdAndRecipeId(UUID userId, UUID recipeId);

    Optional<RecipeLike> findByUserIdAndRecipeId(UUID userId, UUID recipeId);

    @Query("SELECT rl FROM RecipeLike rl WHERE rl.userId = :userId")
    Page<RecipeLike> findAllByUserId(UUID userId, Pageable pageable);
}