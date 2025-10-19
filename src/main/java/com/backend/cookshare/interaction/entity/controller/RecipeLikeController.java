package com.backend.cookshare.interaction.entity.controller;

import com.backend.cookshare.interaction.entity.dto.request.RecipeLikeRequest;
import com.backend.cookshare.interaction.entity.dto.response.RecipeLikeResponse;
import com.backend.cookshare.interaction.entity.sevice.RecipeLikeService;
import com.backend.cookshare.recipe_management.dto.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/likes-ratings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecipeLikeController {
    RecipeLikeService recipeLikeService;
    @PostMapping("/like")
    public ApiResponse<RecipeLikeResponse> likerecipe (@RequestBody RecipeLikeRequest request) {
        return ApiResponse.<RecipeLikeResponse>builder()
                .result(recipeLikeService.likerecipe(request.getRecipeId()))
                .build();
    }
    @GetMapping("/is-liked")
    public ApiResponse<Boolean> isRecipeLiked(@RequestParam UUID recipeId) {
        return ApiResponse.<Boolean>builder()
                .result(recipeLikeService.isRecipeLiked(recipeId))
                .build();
    }
    @DeleteMapping("/unlike")
    public ApiResponse<String> unlikeRecipe(@RequestParam UUID recipeId) {
        recipeLikeService.unlikerecipe(recipeId);
        return ApiResponse.<String>builder()
                .result("Unliked thành công")
                .build();
    }

}
