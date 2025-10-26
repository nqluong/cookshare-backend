package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.dto.RecipeStepResponse;
import com.backend.cookshare.recipe_management.entity.RecipeStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeStepRepository extends JpaRepository<RecipeStep, UUID> {

    @Query(value = """
        SELECT new com.backend.cookshare.recipe_management.dto.RecipeStepResponse(
            rs.stepNumber,
            rs.instruction,
            rs.imageUrl,
            rs.videoUrl,
            rs.estimatedTime,
            rs.tips
        )
        FROM RecipeStep rs
        WHERE rs.recipeId = :recipeId
        ORDER BY rs.stepNumber ASC
        """)
    List<RecipeStepResponse> findByRecipeIdOrderByStepNumber(@Param("recipeId") UUID recipeId);

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
