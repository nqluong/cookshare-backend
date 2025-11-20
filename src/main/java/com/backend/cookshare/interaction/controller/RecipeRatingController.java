package com.backend.cookshare.interaction.controller;

import com.backend.cookshare.common.dto.ApiResponse;
import com.backend.cookshare.interaction.dto.response.RecipeRatingResponse;
import com.backend.cookshare.interaction.sevice.RecipeRatingService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/likes-ratings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecipeRatingController {
    RecipeRatingService recipeRatingService;
    @PostMapping("/rating")
    public ApiResponse<RecipeRatingResponse> rateRecipe(
            @RequestParam UUID recipeId,
            @RequestParam @Min(1) @Max(5) Integer rating) {
        RecipeRatingResponse response = recipeRatingService.ratingrecipe(recipeId, rating);
        return ApiResponse.<RecipeRatingResponse>builder()
                .data(response)
                .build();
    }
    @GetMapping("/is-rated")
    public com.backend.cookshare.recipe_management.dto.ApiResponse<Boolean> isRecipeLiked(@RequestParam UUID recipeId) {
        return com.backend.cookshare.recipe_management.dto.ApiResponse.<Boolean>builder()
                .result(recipeRatingService.isRecipeRated(recipeId))
                .build();
    }
    @GetMapping("/my-rating")
    public ApiResponse<Integer> getMyRating(@RequestParam UUID recipeId) {
        Integer myRating = recipeRatingService.getMyRating(recipeId);
        return ApiResponse.<Integer>builder()
                .data(myRating)
                .message(myRating != null ? "Đã tìm thấy đánh giá của bạn" : "Chưa đánh giá")
                .build();
    }

}
