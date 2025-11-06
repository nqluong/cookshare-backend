package com.backend.cookshare.recommendation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.*;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeRecommendationResponse {

    private List<RecipeRecommendationResponse> featuredRecipes;

    private List<RecipeRecommendationResponse> popularRecipes;

    private List<RecipeRecommendationResponse> newestRecipes;

    private List<RecipeRecommendationResponse> topRatedRecipes;

    private List<RecipeRecommendationResponse> trendingRecipes;

    private List<RecipeRecommendationResponse> dailyRecommendations;
}

