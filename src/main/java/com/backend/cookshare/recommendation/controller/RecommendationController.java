package com.backend.cookshare.recommendation.controller;

import com.backend.cookshare.common.dto.ApiResponse;
import com.backend.cookshare.recommendation.dto.response.HomeRecommendationResponse;
import com.backend.cookshare.recommendation.dto.response.RecipeRecommendationPageResponse;
import com.backend.cookshare.recommendation.dto.response.RecipeRecommendationResponse;
import com.backend.cookshare.recommendation.service.RecommendationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {
    
    private final RecommendationService recommendationService;

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<HomeRecommendationResponse>> getHomeRecommendations() {
        log.info("API: Nhận yêu cầu lấy gợi ý trang chủ");
        
        HomeRecommendationResponse response = recommendationService.getHomeRecommendations();
        
        return ResponseEntity.ok(ApiResponse.<HomeRecommendationResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách gợi ý thành công")
                .data(response)
                .build());
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<RecipeRecommendationResponse>>> getFeaturedRecipes(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("API: Nhận yêu cầu lấy {} công thức nổi bật", limit);
        
        List<RecipeRecommendationResponse> recipes = recommendationService.getFeaturedRecipes(limit);
        
        return ResponseEntity.ok(ApiResponse.<List<RecipeRecommendationResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức nổi bật thành công")
                .data(recipes)
                .build());
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<RecipeRecommendationResponse>>> getPopularRecipes(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("API: Nhận yêu cầu lấy {} công thức phổ biến", limit);
        
        List<RecipeRecommendationResponse> recipes = recommendationService.getPopularRecipes(limit);
        
        return ResponseEntity.ok(ApiResponse.<List<RecipeRecommendationResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức phổ biến thành công")
                .data(recipes)
                .build());
    }

    @GetMapping("/newest")
    public ResponseEntity<ApiResponse<List<RecipeRecommendationResponse>>> getNewestRecipes(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("API: Nhần yêu cầu lấy {} công thức mới nhất", limit);
        
        List<RecipeRecommendationResponse> recipes = recommendationService.getNewestRecipes(limit);
        
        return ResponseEntity.ok(ApiResponse.<List<RecipeRecommendationResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức mới nhất thành công")
                .data(recipes)
                .build());
    }
    

    @GetMapping("/top-rated")
    public ResponseEntity<ApiResponse<List<RecipeRecommendationResponse>>> getTopRatedRecipes(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("API: Nhận yêu cầu lấy {} công thức đánh giá cao", limit);
        
        List<RecipeRecommendationResponse> recipes = recommendationService.getTopRatedRecipes(limit);
        
        return ResponseEntity.ok(ApiResponse.<List<RecipeRecommendationResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức đánh giá cao thành công")
                .data(recipes)
                .build());
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<RecipeRecommendationResponse>>> getTrendingRecipes(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("API: Nhận yêu cầu lấy {} công thức trending", limit);
        
        List<RecipeRecommendationResponse> recipes = recommendationService.getTrendingRecipes(limit);
        
        return ResponseEntity.ok(ApiResponse.<List<RecipeRecommendationResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức trending thành công")
                .data(recipes)
                .build());
    }
    

    @GetMapping("/featured/page")
    public ResponseEntity<ApiResponse<RecipeRecommendationPageResponse>> getFeaturedRecipesWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("API: Nhận yêu cầu lấy công thức nổi bật với phân trang - Page: {}, Size: {}", page, size);
        
        RecipeRecommendationPageResponse response = recommendationService.getFeaturedRecipesWithPagination(page, size);
        
        return ResponseEntity.ok(ApiResponse.<RecipeRecommendationPageResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức nổi bật thành công")
                .data(response)
                .build());
    }
    

    @GetMapping("/popular/page")
    public ResponseEntity<ApiResponse<RecipeRecommendationPageResponse>> getPopularRecipesWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("API: Nhận yêu cầu lấy công thức phổ biến với phân trang - Page: {}, Size: {}", page, size);
        
        RecipeRecommendationPageResponse response = recommendationService.getPopularRecipesWithPagination(page, size);
        
        return ResponseEntity.ok(ApiResponse.<RecipeRecommendationPageResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức phổ biến thành công")
                .data(response)
                .build());
    }

    @GetMapping("/newest/page")
    public ResponseEntity<ApiResponse<RecipeRecommendationPageResponse>> getNewestRecipesWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("API: Nhận yêu cầu lấy công thức mới nhất với phân trang - Page: {}, Size: {}", page, size);
        
        RecipeRecommendationPageResponse response = recommendationService.getNewestRecipesWithPagination(page, size);
        
        return ResponseEntity.ok(ApiResponse.<RecipeRecommendationPageResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức mới nhất thành công")
                .data(response)
                .build());
    }
    

    @GetMapping("/top-rated/page")
    public ResponseEntity<ApiResponse<RecipeRecommendationPageResponse>> getTopRatedRecipesWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("API: Nhận yêu cầu lấy công thức đánh giá cao với phân trang - Page: {}, Size: {}", page, size);
        
        RecipeRecommendationPageResponse response = recommendationService.getTopRatedRecipesWithPagination(page, size);
        
        return ResponseEntity.ok(ApiResponse.<RecipeRecommendationPageResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức đánh giá cao thành công")
                .data(response)
                .build());
    }

    @GetMapping("/trending/page")
    public ResponseEntity<ApiResponse<RecipeRecommendationPageResponse>> getTrendingRecipesWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("API: Nhận yêu cầu lấy công thức trending với phân trang - Page: {}, Size: {}", page, size);
        
        RecipeRecommendationPageResponse response = recommendationService.getTrendingRecipesWithPagination(page, size);
        
        return ResponseEntity.ok(ApiResponse.<RecipeRecommendationPageResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức trending thành công")
                .data(response)
                .build());
    }
}

