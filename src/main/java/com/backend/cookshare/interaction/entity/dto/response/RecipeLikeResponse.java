package com.backend.cookshare.interaction.entity.dto.response;

import com.backend.cookshare.recipe_management.entity.Recipe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeLikeResponse {
    private UUID userId;
    private UUID recipeId;
    private LocalDateTime createdAt;
    private RecipeSummaryResponse recipe;
}
