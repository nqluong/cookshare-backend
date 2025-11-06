package com.backend.cookshare.recommendation.service;

import com.backend.cookshare.recommendation.dto.response.HomeRecommendationResponse;
import com.backend.cookshare.recommendation.dto.response.RecipeRecommendationPageResponse;
import com.backend.cookshare.recommendation.dto.response.RecipeRecommendationResponse;

import java.util.List;
import java.util.UUID;

public interface RecommendationService {
    
    /**
     * Lấy tất cả các danh sách công thức gợi ý cho trang chủ
     * Bao gồm: công thức nổi bật, phổ biến, mới nhất, đánh giá cao, trending
     */
    HomeRecommendationResponse getHomeRecommendations();
    /**
     * Gợi ý 3 công thức theo ngày
     * Mỗi ngày sẽ có các công thức khác nhau
     * Mỗi người dùng có thể có gợi ý giống hoặc khác nhau dựa trên userId
     */
    List<RecipeRecommendationResponse> getDailyRecommendations(UUID userId);
    /**
     * Lấy danh sách công thức nổi bật (được admin đánh dấu)
     * Chỉ lấy các công thức đã được xuất bản và đánh dấu isFeatured = true
     */
    List<RecipeRecommendationResponse> getFeaturedRecipes(int limit);
    
    /**
     * Lấy danh sách công thức phổ biến (nhiều lượt thích và xem)
     * Sắp xếp theo điểm phổ biến = (likeCount * 2) + (viewCount * 0.5)
     */
    List<RecipeRecommendationResponse> getPopularRecipes(int limit);
    
    /**
     * Lấy danh sách công thức mới nhất
     */
    List<RecipeRecommendationResponse> getNewestRecipes(int limit);
    
    /**
     * Lấy danh sách công thức đánh giá cao nhất
     * Chỉ lấy công thức có ít nhất 5 lượt đánh giá
     */
    List<RecipeRecommendationResponse> getTopRatedRecipes(int limit);
    
    /**
     * Lấy danh sách công thức đang trending
     * Tính điểm trending dựa trên tốc độ tăng trưởng lượt xem trong 7 ngày gần đây
     */
    List<RecipeRecommendationResponse> getTrendingRecipes(int limit);
    
    /**
     * Lấy danh sách công thức nổi bật với phân trang
     * Chỉ lấy các công thức đã được xuất bản và đánh dấu isFeatured = true
     */
    RecipeRecommendationPageResponse getFeaturedRecipesWithPagination(int page, int size);
    
    /**
     * Lấy danh sách công thức phổ biến với phân trang
     * Sắp xếp theo điểm phổ biến = (likeCount * 2) + (viewCount * 0.5) + (saveCount * 1.5)
     */
    RecipeRecommendationPageResponse getPopularRecipesWithPagination(int page, int size);
    
    /**
     * Lấy danh sách công thức mới nhất với phân trang
     */
    RecipeRecommendationPageResponse getNewestRecipesWithPagination(int page, int size);
    
    /**
     * Lấy danh sách công thức đánh giá cao với phân trang
     * Chỉ lấy công thức có ít nhất 5 lượt đánh giá
     */
    RecipeRecommendationPageResponse getTopRatedRecipesWithPagination(int page, int size);
    
    /**
     * Lấy danh sách công thức trending với phân trang
     * Tính điểm trending dựa trên: viewCount + (likeCount * 3) + (ratingCount * 2)
     */
    RecipeRecommendationPageResponse getTrendingRecipesWithPagination(int page, int size);
}

