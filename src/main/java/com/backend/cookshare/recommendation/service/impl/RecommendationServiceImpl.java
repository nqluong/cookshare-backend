package com.backend.cookshare.recommendation.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {
    
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final FirebaseStorageService firebaseStorageService;
    private final Executor executor;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;
    private static final int MIN_RATING_COUNT = 5;

    public RecommendationServiceImpl(
            RecipeRepository recipeRepository,
            UserRepository userRepository,
            FirebaseStorageService firebaseStorageService,
            @Qualifier("taskExecutor") Executor executor) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.firebaseStorageService = firebaseStorageService;
        this.executor = executor;
    }
    
    @Override
    public HomeRecommendationResponse getHomeRecommendations() {
        log.info("Bắt đầu lấy tất cả gợi ý công thức cho trang chủ");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user= userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        UUID userId= user.getUserId();
        try {
            // Sử dụng đa luồng để lấy tất cả recommendations song song
            CompletableFuture<List<RecipeRecommendationResponse>> featuredFuture =
                CompletableFuture.supplyAsync(() -> getFeaturedRecipes(DEFAULT_LIMIT), executor);

            CompletableFuture<List<RecipeRecommendationResponse>> popularFuture =
                CompletableFuture.supplyAsync(() -> getPopularRecipes(DEFAULT_LIMIT), executor);

            CompletableFuture<List<RecipeRecommendationResponse>> newestFuture =
                CompletableFuture.supplyAsync(() -> getNewestRecipes(DEFAULT_LIMIT), executor);

            CompletableFuture<List<RecipeRecommendationResponse>> topRatedFuture =
                CompletableFuture.supplyAsync(() -> getTopRatedRecipes(DEFAULT_LIMIT), executor);

            CompletableFuture<List<RecipeRecommendationResponse>> trendingFuture =
                CompletableFuture.supplyAsync(() -> getTrendingRecipes(DEFAULT_LIMIT), executor);
            CompletableFuture<List<RecipeRecommendationResponse>> dailyFuture =
                    CompletableFuture.supplyAsync(() -> getDailyRecommendations(userId), executor);

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                featuredFuture, popularFuture, newestFuture, topRatedFuture, trendingFuture);

            allFutures.join();

            List<RecipeRecommendationResponse> featured = featuredFuture.join();
            List<RecipeRecommendationResponse> popular = popularFuture.join();
            List<RecipeRecommendationResponse> newest = newestFuture.join();
            List<RecipeRecommendationResponse> topRated = topRatedFuture.join();
            List<RecipeRecommendationResponse> trending = trendingFuture.join();
            log.info("Đã lấy thành công tất cả gợi ý: {} featured, {} popular, {} newest, {} topRated, {} trending",
                    featured.size(), popular.size(), newest.size(), topRated.size(), trending.size());

            return HomeRecommendationResponse.builder()
                    .featuredRecipes(featured)
                    .popularRecipes(popular)
                    .newestRecipes(newest)
                    .topRatedRecipes(topRated)
                    .trendingRecipes(trending)
                    .dailyRecommendations(dailyFuture.join())
                    .build();

        } catch (Exception e) {
            log.error("Lỗi khi lấy gợi ý trang chủ: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lấy danh sách gợi ý công thức");
        }
    }

    @Override
    public List<RecipeRecommendationResponse> getDailyRecommendations(String userName) {
        User user= userRepository.findByUsername(userName)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        try {
            // Lấy ngày hiện tại (chỉ lấy ngày, không tính giờ)
            LocalDate today = LocalDate.now();

            // Tạo seed dựa trên ngày và userId để đảm bảo tính đa dạng
            long seed = generateDailySeed(today, user.getUserId());

            // Lấy tất cả công thức đã publish
            List<Recipe> allPublishedRecipes = recipeRepository.findAllPublishedRecipes();

            if (allPublishedRecipes.isEmpty()) {
                log.warn("Không có công thức nào được publish");
                return List.of();
            }

            // Shuffle danh sách với seed để đảm bảo mỗi ngày có thứ tự khác nhau
            List<Recipe> shuffledRecipes = shuffleWithSeed(allPublishedRecipes, seed);

            // Lấy 3 công thức đầu tiên sau khi shuffle
            List<Recipe> selectedRecipes = shuffledRecipes.stream()
                    .limit(3)
                    .collect(Collectors.toList());

            log.info("Đã chọn {} công thức cho gợi ý hàng ngày", selectedRecipes.size());

            return convertToRecommendationResponses(selectedRecipes);

        } catch (Exception e) {
            log.error("Lỗi khi lấy gợi ý công thức theo ngày: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Không thể lấy danh sách gợi ý công thức theo ngày");
        }
    }

    @Override
    public List<RecipeRecommendationResponse> getDailyRecommendations(UUID userId) {
        log.info("Lấy gợi ý công thức theo ngày cho user: {}", userId);

        try {
            // Lấy ngày hiện tại (chỉ lấy ngày, không tính giờ)
            LocalDate today = LocalDate.now();

            // Tạo seed dựa trên ngày và userId để đảm bảo tính đa dạng
            long seed = generateDailySeed(today, userId);

            // Lấy tất cả công thức đã publish
            List<Recipe> allPublishedRecipes = recipeRepository.findAllPublishedRecipes();

            if (allPublishedRecipes.isEmpty()) {
                log.warn("Không có công thức nào được publish");
                return List.of();
            }

            // Shuffle danh sách với seed để đảm bảo mỗi ngày có thứ tự khác nhau
            List<Recipe> shuffledRecipes = shuffleWithSeed(allPublishedRecipes, seed);

            // Lấy 3 công thức đầu tiên sau khi shuffle
            List<Recipe> selectedRecipes = shuffledRecipes.stream()
                    .limit(3)
                    .collect(Collectors.toList());

            log.info("Đã chọn {} công thức cho gợi ý hàng ngày", selectedRecipes.size());

            return convertToRecommendationResponses(selectedRecipes);

        } catch (Exception e) {
            log.error("Lỗi khi lấy gợi ý công thức theo ngày: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Không thể lấy danh sách gợi ý công thức theo ngày");
        }
    }

    /**
     * Tạo seed cho việc shuffle dựa trên ngày và userId
     * Công thức: (năm * 10000 + tháng * 100 + ngày) XOR hash(userId)
     */
    private long generateDailySeed(LocalDate date, UUID userId) {
        // Tạo base seed từ ngày
        long dateSeed = date.getYear() * 10000L + date.getMonthValue() * 100L + date.getDayOfMonth();

        // Nếu có userId, XOR với hash của userId để tạo sự đa dạng
        if (userId != null) {
            long userSeed = Math.abs(userId.hashCode());
            // Sử dụng XOR để kết hợp cả ngày và userId
            return dateSeed ^ userSeed;
        }

        // Nếu không có userId, chỉ dùng dateSeed
        return dateSeed;
    }

    /**
     * Shuffle danh sách với seed cố định
     * Sử dụng Random với seed để đảm bảo kết quả shuffle giống nhau với cùng seed
     */
    private List<Recipe> shuffleWithSeed(List<Recipe> recipes, long seed) {
        List<Recipe> shuffled = new ArrayList<>(recipes);
        Random random = new Random(seed);

        // Fisher-Yates shuffle algorithm
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Recipe temp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, temp);
        }

        return shuffled;
    }
    
    @Override
    public List<RecipeRecommendationResponse> getFeaturedRecipes(int limit) {
        log.info("Lấy {} công thức nổi bật", limit);

        validateLimit(limit);
        
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<Recipe> recipes = recipeRepository.findFeaturedRecipes(pageable).getContent();
            
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
            Pageable pageable = PageRequest.of(0, limit);
            List<Recipe> recipes = recipeRepository.findPopularRecipes(pageable).getContent();
            
            log.info("Tìm thấy {} công thức phổ biến", recipes.size());
            
            return convertToRecommendationResponses(recipes);
            
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
            Pageable pageable = PageRequest.of(0, limit);
            List<Recipe> recipes = recipeRepository.findNewestRecipes(pageable).getContent();
            
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
            Pageable pageable = PageRequest.of(0, limit);
            List<Recipe> recipes = recipeRepository.findTopRatedRecipes(MIN_RATING_COUNT, pageable).getContent();
            
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
            Pageable pageable = PageRequest.of(0, limit);
            List<Recipe> recipes = recipeRepository.findTrendingRecipes(pageable).getContent();
            
            log.info("Tìm thấy {} công thức trending", recipes.size());
            
            return convertToRecommendationResponses(recipes);
            
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
    
    

    private List<RecipeRecommendationResponse> convertToRecommendationResponses(List<Recipe> recipes) {
        if (recipes.isEmpty()) {
            return List.of();
        }
        
        // Batch query để lấy user names
        List<UUID> userIds = recipes.stream()
                .map(Recipe::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, String> userNameMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getUserId, User::getFullName));

        // Convert recipes to response objects
        return recipes.stream()
                .map(recipe -> RecipeRecommendationResponse.builder()
                        .recipeId(recipe.getRecipeId())
                        .title(recipe.getTitle())
                        .slug(recipe.getSlug())
                        .description(recipe.getDescription())
                        .featuredImage(firebaseStorageService.convertPathToFirebaseUrl(recipe.getFeaturedImage()))
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
            Pageable pageable = PageRequest.of(page, size);
            Page<Recipe> recipePage = recipeRepository.findFeaturedRecipes(pageable);
            
            return createPageResponseFromPage(recipePage);
            
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
            Pageable pageable = PageRequest.of(page, size);
            Page<Recipe> recipePage = recipeRepository.findPopularRecipes(pageable);
            
            return createPageResponseFromPage(recipePage);
            
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
            Pageable pageable = PageRequest.of(page, size);
            Page<Recipe> recipePage = recipeRepository.findNewestRecipes(pageable);
            
            return createPageResponseFromPage(recipePage);
            
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
            Pageable pageable = PageRequest.of(page, size);
            Page<Recipe> recipePage = recipeRepository.findTopRatedRecipes(MIN_RATING_COUNT, pageable);
            
            return createPageResponseFromPage(recipePage);
            
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
            Pageable pageable = PageRequest.of(page, size);
            Page<Recipe> recipePage = recipeRepository.findTrendingRecipes(pageable);
            
            return createPageResponseFromPage(recipePage);
            
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

    private RecipeRecommendationPageResponse createPageResponseFromPage(Page<Recipe> recipePage) {
        List<RecipeRecommendationResponse> content = convertToRecommendationResponses(recipePage.getContent());
        return RecipeRecommendationPageResponse.builder()
                .content(content)
                .currentPage(recipePage.getNumber())
                .pageSize(recipePage.getSize())
                .totalElements(recipePage.getTotalElements())
                .totalPages(recipePage.getTotalPages())
                .hasNext(recipePage.hasNext())
                .hasPrevious(recipePage.hasPrevious())
                .isFirst(recipePage.isFirst())
                .isLast(recipePage.isLast())
                .build();
    }
}

