package com.backend.cookshare.interaction.dto.response;

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
