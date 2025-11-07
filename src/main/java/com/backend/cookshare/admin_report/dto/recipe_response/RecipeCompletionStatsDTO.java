package com.backend.cookshare.admin_report.dto.recipe_response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCompletionStatsDTO {
    private Long totalRecipes;
    private Long withDescription;
    private Long withImage;
    private Long withVideo;
    private Long withIngredients;
    private Long withSteps;
    private Long completeRecipes;
    private Double completionRate;
}
