package com.backend.cookshare.recommendation.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.recommendation.dto.response.HomeRecommendationResponse;
import com.backend.cookshare.recommendation.dto.response.RecipeRecommendationPageResponse;
import com.backend.cookshare.recommendation.dto.response.RecipeRecommendationResponse;
import com.backend.cookshare.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {
    
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;
    private static final int MIN_RATING_COUNT = 5;
    
    @Override
    public HomeRecommendationResponse getHomeRecommendations() {
        log.info("Bắt đầu lấy tất cả gợi ý công thức cho trang chủ");
        
        try {
            List<RecipeRecommendationResponse> featured = getFeaturedRecipes(DEFAULT_LIMIT);
            List<RecipeRecommendationResponse> popular = getPopularRecipes(DEFAULT_LIMIT);
            List<RecipeRecommendationResponse> newest = getNewestRecipes(DEFAULT_LIMIT);
            List<RecipeRecommendationResponse> topRated = getTopRatedRecipes(DEFAULT_LIMIT);
            List<RecipeRecommendationResponse> trending = getTrendingRecipes(DEFAULT_LIMIT);
            
            log.info("Đã lấy thành công tất cả gợi ý: {} featured, {} popular, {} newest, {} topRated, {} trending",
                    featured.size(), popular.size(), newest.size(), topRated.size(), trending.size());
            
            return HomeRecommendationResponse.builder()
                    .featuredRecipes(featured)
                    .popularRecipes(popular)
                    .newestRecipes(newest)
                    .topRatedRecipes(topRated)
                    .trendingRecipes(trending)
                    .build();
                    
        } catch (Exception e) {
            log.error("Lỗi khi lấy gợi ý trang chủ: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách gợi ý công thức");
        }
    }
    
    @Override
    public List<RecipeRecommendationResponse> getFeaturedRecipes(int limit) {
        log.info("Lấy {} công thức nổi bật", limit);

        validateLimit(limit);
        
        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"));

            List<Recipe> recipes = recipeRepository.findAll(pageable)
                    .stream()
                    .filter(recipe -> Boolean.TRUE.equals(recipe.getIsPublished()) && 
                                     Boolean.TRUE.equals(recipe.getIsFeatured()))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            log.info("Tìm thấy {} công thức nổi bật", recipes.size());
            
            return convertToRecommendationResponses(recipes);
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy công thức nổi bật: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách công thức nổi bật");
        }
    }
    
    @Override
    public List<RecipeRecommendationResponse> getPopularRecipes(int limit) {
        log.info("Lấy {} công thức phổ biến", limit);

        validateLimit(limit);
        
        try {
            List<Recipe> recipes = recipeRepository.findAll()
                    .stream()
                    .filter(recipe -> Boolean.TRUE.equals(recipe.getIsPublished()))
                    .collect(Collectors.toList());
            
            // Tính điểm phổ biến và sắp xếp
            // Công thức: popularityScore = (likeCount * 2) + (viewCount * 0.5) + (saveCount * 1.5)
            List<Recipe> popularRecipes = recipes.stream()
                    .sorted((r1, r2) -> {
                        double score1 = calculatePopularityScore(r1);
                        double score2 = calculatePopularityScore(r2);
                        return Double.compare(score2, score1);
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
            
            log.info("Tìm thấy {} công thức phổ biến", popularRecipes.size());
            
            return convertToRecommendationResponses(popularRecipes);
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy công thức phổ biến: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách công thức phổ biến");
        }
    }
    
    @Override
    public List<RecipeRecommendationResponse> getNewestRecipes(int limit) {
        log.info("Lấy {} công thức mới nhất", limit);

        validateLimit(limit);
        
        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

            List<Recipe> recipes = recipeRepository.findAll(pageable)
                    .stream()
                    .filter(recipe -> Boolean.TRUE.equals(recipe.getIsPublished()))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            log.info("Tìm thấy {} công thức mới nhất", recipes.size());
            
            return convertToRecommendationResponses(recipes);
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy công thức mới nhất: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách công thức mới nhất");
        }
    }
    
    @Override
    public List<RecipeRecommendationResponse> getTopRatedRecipes(int limit) {
        log.info("Lấy {} công thức đánh giá cao nhất", limit);

        validateLimit(limit);
        
        try {
            // Lấy các công thức đã xuất bản và có đủ số lượt đánh giá
            List<Recipe> recipes = recipeRepository.findAll()
                    .stream()
                    .filter(recipe -> Boolean.TRUE.equals(recipe.getIsPublished()) && 
                                     recipe.getRatingCount() != null &&
                                     recipe.getRatingCount() >= MIN_RATING_COUNT)
                    .sorted((r1, r2) -> {
                        BigDecimal rating1 = r1.getAverageRating() != null ? r1.getAverageRating() : BigDecimal.ZERO;
                        BigDecimal rating2 = r2.getAverageRating() != null ? r2.getAverageRating() : BigDecimal.ZERO;
                        int ratingCompare = rating2.compareTo(rating1);

                        if (ratingCompare == 0) {
                            return Integer.compare(
                                r2.getRatingCount() != null ? r2.getRatingCount() : 0,
                                r1.getRatingCount() != null ? r1.getRatingCount() : 0
                            );
                        }
                        return ratingCompare;
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
            
            log.info("Tìm thấy {} công thức đánh giá cao", recipes.size());
            
            return convertToRecommendationResponses(recipes);
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy công thức đánh giá cao: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách công thức đánh giá cao");
        }
    }
    
    @Override
    public List<RecipeRecommendationResponse> getTrendingRecipes(int limit) {
        log.info("Lấy {} công thức đang trending", limit);

        validateLimit(limit);
        
        try {
            List<Recipe> recipes = recipeRepository.findAll()
                    .stream()
                    .filter(recipe -> Boolean.TRUE.equals(recipe.getIsPublished()))
                    .collect(Collectors.toList());
            
            // Tính điểm trending
            // Công thức: trendingScore = viewCount + (likeCount * 3) + (ratingCount * 2)
            // Ưu tiên công thức mới hơn
            List<Recipe> trendingRecipes = recipes.stream()
                    .sorted((r1, r2) -> {
                        double score1 = calculateTrendingScore(r1);
                        double score2 = calculateTrendingScore(r2);
                        
                        int scoreCompare = Double.compare(score2, score1);

                        if (scoreCompare == 0) {
                            LocalDateTime time1 = r1.getCreatedAt() != null ? r1.getCreatedAt() : LocalDateTime.MIN;
                            LocalDateTime time2 = r2.getCreatedAt() != null ? r2.getCreatedAt() : LocalDateTime.MIN;
                            return time2.compareTo(time1);
                        }
                        return scoreCompare;
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
            
            log.info("Tìm thấy {} công thức trending", trendingRecipes.size());
            
            return convertToRecommendationResponses(trendingRecipes);
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy công thức trending: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách công thức trending");
        }
    }
    
    /**
     * Validate giá trị limit đầu vào
     */
    private void validateLimit(int limit) {
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            log.warn("Limit không hợp lệ: {}. Phải trong khoảng {} - {}", limit, MIN_LIMIT, MAX_LIMIT);
            throw new CustomException(
                ErrorCode.VALIDATION_ERROR, 
                String.format("Limit phải trong khoảng %d - %d", MIN_LIMIT, MAX_LIMIT)
            );
        }
    }
    
    /**
     * Tính điểm phổ biến của công thức
     * Công thức: (likeCount * 2) + (viewCount * 0.5) + (saveCount * 1.5)
     */
    private double calculatePopularityScore(Recipe recipe) {
        int likeCount = recipe.getLikeCount() != null ? recipe.getLikeCount() : 0;
        int viewCount = recipe.getViewCount() != null ? recipe.getViewCount() : 0;
        int saveCount = recipe.getSaveCount() != null ? recipe.getSaveCount() : 0;
        
        return (likeCount * 2.0) + (viewCount * 0.5) + (saveCount * 1.5);
    }
    
    /**
     * Công thức: viewCount + (likeCount * 3) + (ratingCount * 2)
     */
    private double calculateTrendingScore(Recipe recipe) {
        int viewCount = recipe.getViewCount() != null ? recipe.getViewCount() : 0;
        int likeCount = recipe.getLikeCount() != null ? recipe.getLikeCount() : 0;
        int ratingCount = recipe.getRatingCount() != null ? recipe.getRatingCount() : 0;
        
        return viewCount + (likeCount * 3.0) + (ratingCount * 2.0);
    }
    

    private List<RecipeRecommendationResponse> convertToRecommendationResponses(List<Recipe> recipes) {
        List<UUID> userIds = recipes.stream()
                .map(Recipe::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, String> userNameMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getUserId, User::getFullName));

        return recipes.stream()
                .map(recipe -> RecipeRecommendationResponse.builder()
                        .recipeId(recipe.getRecipeId())
                        .title(recipe.getTitle())
                        .slug(recipe.getSlug())
                        .description(recipe.getDescription())
                        .featuredImage(recipe.getFeaturedImage())
                        .prepTime(recipe.getPrepTime())
                        .cookTime(recipe.getCookTime())
                        .servings(recipe.getServings())
                        .difficulty(recipe.getDifficulty())
                        .userId(recipe.getUserId())
                        .userName(userNameMap.getOrDefault(recipe.getUserId(), "Unknown"))
                        .viewCount(recipe.getViewCount())
                        .saveCount(recipe.getSaveCount())
                        .likeCount(recipe.getLikeCount())
                        .averageRating(recipe.getAverageRating())
                        .ratingCount(recipe.getRatingCount())
                        .isFeatured(recipe.getIsFeatured())
                        .isPublished(recipe.getIsPublished())
                        .createdAt(recipe.getCreatedAt())
                        .updatedAt(recipe.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    public RecipeRecommendationPageResponse getFeaturedRecipesWithPagination(int page, int size) {
        log.info("Lấy công thức nổi bật với phân trang - Trang: {}, Size: {}", page, size);

        validatePaginationParams(page, size);
        
        try {
            List<Recipe> allFeaturedRecipes = recipeRepository.findAll()
                    .stream()
                    .filter(recipe -> Boolean.TRUE.equals(recipe.getIsPublished()) && 
                                     Boolean.TRUE.equals(recipe.getIsFeatured()))
                    .sorted((r1, r2) -> {
                        LocalDateTime time1 = r1.getCreatedAt() != null ? r1.getCreatedAt() : LocalDateTime.MIN;
                        LocalDateTime time2 = r2.getCreatedAt() != null ? r2.getCreatedAt() : LocalDateTime.MIN;
                        return time2.compareTo(time1);
                    })
                    .collect(Collectors.toList());

            return createPageResponse(allFeaturedRecipes, page, size);
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy công thức nổi bật với phân trang: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách công thức nổi bật");
        }
    }
    
    @Override
    public RecipeRecommendationPageResponse getPopularRecipesWithPagination(int page, int size) {
        log.info("Lấy công thức phổ biến với phân trang - Trang: {}, Size: {}", page, size);

        validatePaginationParams(page, size);
        
        try {
            List<Recipe> allPopularRecipes = recipeRepository.findAll()
                    .stream()
                    .filter(recipe -> Boolean.TRUE.equals(recipe.getIsPublished()))
                    .sorted((r1, r2) -> {
                        double score1 = calculatePopularityScore(r1);
                        double score2 = calculatePopularityScore(r2);
                        return Double.compare(score2, score1);
                    })
                    .collect(Collectors.toList());

            return createPageResponse(allPopularRecipes, page, size);
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy công thức phổ biến với phân trang: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách công thức phổ biến");
        }
    }
    
    @Override
    public RecipeRecommendationPageResponse getNewestRecipesWithPagination(int page, int size) {
        log.info("Lấy công thức mới nhất với phân trang - Trang: {}, Size: {}", page, size);

        validatePaginationParams(page, size);
        
        try {
            List<Recipe> allNewestRecipes = recipeRepository.findAll()
                    .stream()
                    .filter(recipe -> Boolean.TRUE.equals(recipe.getIsPublished()))
                    .sorted((r1, r2) -> {
                        LocalDateTime time1 = r1.getCreatedAt() != null ? r1.getCreatedAt() : LocalDateTime.MIN;
                        LocalDateTime time2 = r2.getCreatedAt() != null ? r2.getCreatedAt() : LocalDateTime.MIN;
                        return time2.compareTo(time1);
                    })
                    .collect(Collectors.toList());

            return createPageResponse(allNewestRecipes, page, size);
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy công thức mới nhất với phân trang: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách công thức mới nhất");
        }
    }
    
    @Override
    public RecipeRecommendationPageResponse getTopRatedRecipesWithPagination(int page, int size) {
        log.info("Lấy công thức đánh giá cao với phân trang - Trang: {}, Size: {}", page, size);

        validatePaginationParams(page, size);
        
        try {
            List<Recipe> allTopRatedRecipes = recipeRepository.findAll()
                    .stream()
                    .filter(recipe -> Boolean.TRUE.equals(recipe.getIsPublished()) && 
                                     recipe.getRatingCount() != null &&
                                     recipe.getRatingCount() >= MIN_RATING_COUNT)
                    .sorted((r1, r2) -> {
                        // Sắp xếp theo averageRating giảm dần
                        BigDecimal rating1 = r1.getAverageRating() != null ? r1.getAverageRating() : BigDecimal.ZERO;
                        BigDecimal rating2 = r2.getAverageRating() != null ? r2.getAverageRating() : BigDecimal.ZERO;
                        int ratingCompare = rating2.compareTo(rating1);

                        if (ratingCompare == 0) {
                            return Integer.compare(
                                r2.getRatingCount() != null ? r2.getRatingCount() : 0,
                                r1.getRatingCount() != null ? r1.getRatingCount() : 0
                            );
                        }
                        return ratingCompare;
                    })
                    .collect(Collectors.toList());

            return createPageResponse(allTopRatedRecipes, page, size);
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy công thức đánh giá cao với phân trang: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách công thức đánh giá cao");
        }
    }
    
    @Override
    public RecipeRecommendationPageResponse getTrendingRecipesWithPagination(int page, int size) {
        log.info("Lấy công thức trending với phân trang - Trang: {}, Size: {}", page, size);

        validatePaginationParams(page, size);
        
        try {
            // Lấy tất cả công thức đã xuất bản và sắp xếp theo điểm trending
            List<Recipe> allTrendingRecipes = recipeRepository.findAll()
                    .stream()
                    .filter(recipe -> Boolean.TRUE.equals(recipe.getIsPublished()))
                    .sorted((r1, r2) -> {
                        double score1 = calculateTrendingScore(r1);
                        double score2 = calculateTrendingScore(r2);
                        
                        int scoreCompare = Double.compare(score2, score1);
                        
                        // Nếu điểm bằng nhau, ưu tiên công thức mới hơn
                        if (scoreCompare == 0) {
                            LocalDateTime time1 = r1.getCreatedAt() != null ? r1.getCreatedAt() : LocalDateTime.MIN;
                            LocalDateTime time2 = r2.getCreatedAt() != null ? r2.getCreatedAt() : LocalDateTime.MIN;
                            return time2.compareTo(time1);
                        }
                        return scoreCompare;
                    })
                    .collect(Collectors.toList());

            return createPageResponse(allTrendingRecipes, page, size);
            
        } catch (Exception e) {
            log.error("Lỗi khi lấy công thức trending với phân trang: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách công thức trending");
        }
    }

    private void validatePaginationParams(int page, int size) {
        if (page < 0) {
            log.warn("Số trang không hợp lệ: {}. Phải >= 0", page);
            throw new CustomException(
                ErrorCode.VALIDATION_ERROR, 
                "Số trang phải >= 0"
            );
        }
        
        if (size < 1 || size > 100) {
            log.warn("Kích thước trang không hợp lệ: {}. Phải trong khoảng 1-100", size);
            throw new CustomException(
                ErrorCode.VALIDATION_ERROR, 
                "Kích thước trang phải trong khoảng 1-100"
            );
        }
    }

    private RecipeRecommendationPageResponse createPageResponse(List<Recipe> allRecipes, int page, int size) {
        long totalElements = allRecipes.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allRecipes.size());

        List<Recipe> pageRecipes = startIndex < allRecipes.size() 
                ? allRecipes.subList(startIndex, endIndex)
                : List.of();

        List<RecipeRecommendationResponse> content = convertToRecommendationResponses(pageRecipes);
        return RecipeRecommendationPageResponse.builder()
                .content(content)
                .currentPage(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .isFirst(page == 0)
                .isLast(page >= totalPages - 1)
                .build();
    }
}

