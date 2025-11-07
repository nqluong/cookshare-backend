package com.backend.cookshare.admin_report.dto.recipe_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeOverviewDTO {
     Long totalRecipes;
     Long newRecipesToday;
     Long newRecipesThisWeek;
     Long newRecipesThisMonth;
     Double growthRateDaily;
     Double growthRateWeekly;
     Double growthRateMonthly;
     Map<String, Long> recipesByCategory;
     Map<String, Long> recipesByDifficulty;
}
