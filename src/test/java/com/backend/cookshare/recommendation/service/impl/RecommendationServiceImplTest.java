package com.backend.cookshare.recommendation.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.enums.Difficulty;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.recommendation.dto.response.HomeRecommendationResponse;
import com.backend.cookshare.recommendation.dto.response.RecipeRecommendationPageResponse;
import com.backend.cookshare.recommendation.dto.response.RecipeRecommendationResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.quality.Strictness;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecommendationServiceImplTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FirebaseStorageService firebaseStorageService;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private User testUser;
    private List<Recipe> mockRecipes;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");

        Recipe recipe = new Recipe();
        recipe.setRecipeId(UUID.randomUUID());
        recipe.setTitle("Test Recipe");
        recipe.setSlug("test-recipe");
        recipe.setDescription("Delicious test");
        recipe.setFeaturedImage("path/to/image.jpg");
        recipe.setPrepTime(10);
        recipe.setCookTime(20);
        recipe.setServings(4);
        recipe.setDifficulty(Difficulty.EASY);
        recipe.setUserId(testUser.getUserId());
        recipe.setViewCount(100);
        recipe.setSaveCount(50);
        recipe.setLikeCount(30);
        recipe.setAverageRating(new BigDecimal("4.5"));
        recipe.setRatingCount(20);
        recipe.setIsPublished(true);
        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());

        mockRecipes = List.of(recipe);

        // Mock Firebase URL conversion
        when(firebaseStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase/url/image.jpg");

        // Mock user lookup in batch
        when(userRepository.findAllById(anyCollection()))
                .thenReturn(List.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupAuthentication() {
        Authentication auth = new UsernamePasswordAuthenticationToken("testuser", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("getHomeRecommendations - success with all sections")
    void getHomeRecommendations_success() {
        setupAuthentication();

        Page<Recipe> page = new PageImpl<>(mockRecipes, PageRequest.of(0, 10), 1);

        when(recipeRepository.findFeaturedRecipes(any(Pageable.class))).thenReturn(page);
        when(recipeRepository.findPopularRecipes(any(Pageable.class))).thenReturn(page);
        when(recipeRepository.findNewestRecipes(any(Pageable.class))).thenReturn(page);
        when(recipeRepository.findTopRatedRecipes(anyInt(), any(Pageable.class))).thenReturn(page);
        when(recipeRepository.findTrendingRecipes(any(Pageable.class))).thenReturn(page);
        when(recipeRepository.findAllPublishedRecipes()).thenReturn(mockRecipes);

        HomeRecommendationResponse response = recommendationService.getHomeRecommendations();

        assertNotNull(response);
        assertEquals(1, response.getFeaturedRecipes().size());
        assertEquals(1, response.getPopularRecipes().size());
        assertEquals(1, response.getNewestRecipes().size());
        assertEquals(1, response.getTopRatedRecipes().size());
        assertEquals(1, response.getTrendingRecipes().size());
        assertEquals(1, response.getDailyRecommendations().size()); // limit 3 but only 1 recipe available

        verify(recipeRepository, times(1)).findFeaturedRecipes(any(Pageable.class));
        verify(recipeRepository, times(1)).findPopularRecipes(any(Pageable.class));
        verify(recipeRepository, times(1)).findNewestRecipes(any(Pageable.class));
        verify(recipeRepository, times(1)).findTopRatedRecipes(anyInt(), any(Pageable.class));
        verify(recipeRepository, times(1)).findTrendingRecipes(any(Pageable.class));
        verify(recipeRepository, times(1)).findAllPublishedRecipes();
    }

    @Test
    @DisplayName("getHomeRecommendations - user not found throws exception")
    void getHomeRecommendations_userNotFound() {
        Authentication auth = new UsernamePasswordAuthenticationToken("unknown", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> recommendationService.getHomeRecommendations());

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("getDailyRecommendations - deterministic shuffle with same seed")
    void getDailyRecommendations_deterministic() {
        UUID userId = testUser.getUserId();
        when(recipeRepository.findAllPublishedRecipes()).thenReturn(mockRecipes);

        List<RecipeRecommendationResponse> result1 = recommendationService.getDailyRecommendations(userId);
        List<RecipeRecommendationResponse> result2 = recommendationService.getDailyRecommendations(userId);

        assertEquals(result1.size(), result2.size());
        assertEquals(result1.get(0).getRecipeId(), result2.get(0).getRecipeId()); // same order due to seed
    }

    @Test
    @DisplayName("getDailyRecommendations - empty when no published recipes")
    void getDailyRecommendations_emptyList() {
        when(recipeRepository.findAllPublishedRecipes()).thenReturn(List.of());

        List<RecipeRecommendationResponse> result = recommendationService.getDailyRecommendations(testUser.getUserId());

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 30, 50})
    @DisplayName("getFeaturedRecipes - valid limit")
    void getFeaturedRecipes_validLimit(int limit) {
        Page<Recipe> page = new PageImpl<>(mockRecipes);
        when(recipeRepository.findFeaturedRecipes(any(Pageable.class))).thenReturn(page);

        List<RecipeRecommendationResponse> result = recommendationService.getFeaturedRecipes(limit);

        assertEquals(1, result.size());
        verify(recipeRepository).findFeaturedRecipes(PageRequest.of(0, limit));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 51, 100})
    @DisplayName("validateLimit - throws exception for invalid limit")
    void validateLimit_invalid(int limit) {
        CustomException exception = assertThrows(CustomException.class,
                () -> recommendationService.getFeaturedRecipes(limit));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("getTopRatedRecipes - calls with MIN_RATING_COUNT")
    void getTopRatedRecipes_callsWithMinRatingCount() {
        Page<Recipe> page = new PageImpl<>(mockRecipes);
        when(recipeRepository.findTopRatedRecipes(eq(5), any(Pageable.class))).thenReturn(page);

        recommendationService.getTopRatedRecipes(10);

        verify(recipeRepository).findTopRatedRecipes(5, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("getFeaturedRecipesWithPagination - valid pagination")
    void getFeaturedRecipesWithPagination_valid() {
        Page<Recipe> page = new PageImpl<>(mockRecipes, PageRequest.of(0, 20), 50);
        when(recipeRepository.findFeaturedRecipes(any(Pageable.class))).thenReturn(page);

        RecipeRecommendationPageResponse response = recommendationService.getFeaturedRecipesWithPagination(0, 20);

        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(20, response.getPageSize());
        assertEquals(50, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -5})
    @DisplayName("validatePaginationParams - invalid page throws")
    void validatePaginationParams_invalidPage(int page) {
        CustomException exception = assertThrows(CustomException.class,
                () -> recommendationService.getFeaturedRecipesWithPagination(page, 20));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101, 200})
    @DisplayName("validatePaginationParams - invalid size throws")
    void validatePaginationParams_invalidSize(int size) {
        CustomException exception = assertThrows(CustomException.class,
                () -> recommendationService.getFeaturedRecipesWithPagination(0, size));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("convertToRecommendationResponses - maps user name correctly")
    void convertToRecommendationResponses_mapsUserName() throws Exception {
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(testUser));

        Method convertMethod = RecommendationServiceImpl.class
                .getDeclaredMethod("convertToRecommendationResponses", List.class);
        convertMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<RecipeRecommendationResponse> result = (List<RecipeRecommendationResponse>)
                convertMethod.invoke(recommendationService, mockRecipes);

        assertEquals("Test User", result.get(0).getUserName());
        assertEquals("https://firebase/url/image.jpg", result.get(0).getFeaturedImage());
    }

    @Test
    @DisplayName("generateDailySeed - same date and userId gives same seed")
    void generateDailySeed_deterministic() throws Exception {
        Method method = RecommendationServiceImpl.class
                .getDeclaredMethod("generateDailySeed", LocalDate.class, UUID.class);
        method.setAccessible(true);

        LocalDate today = LocalDate.of(2025, 12, 13);
        UUID userId = testUser.getUserId();

        long seed1 = (long) method.invoke(recommendationService, today, userId);
        long seed2 = (long) method.invoke(recommendationService, today, userId);

        assertEquals(seed1, seed2);
    }

    @Test
    @DisplayName("shuffleWithSeed - same seed gives same order")
    void shuffleWithSeed_deterministic() throws Exception {
        List<Recipe> recipes = new ArrayList<>(mockRecipes);
        recipes.addAll(mockRecipes); // duplicate to have multiple items

        Method method = RecommendationServiceImpl.class
                .getDeclaredMethod("shuffleWithSeed", List.class, long.class);
        method.setAccessible(true);

        List<Recipe> shuffled1 = (List<Recipe>) method.invoke(recommendationService, recipes, 12345L);
        List<Recipe> shuffled2 = (List<Recipe>) method.invoke(recommendationService, recipes, 12345L);

        assertEquals(shuffled1.size(), shuffled2.size());
        for (int i = 0; i < shuffled1.size(); i++) {
            assertEquals(shuffled1.get(i).getRecipeId(), shuffled2.get(i).getRecipeId());
        }
    }

    @Test
    @DisplayName("Parallel execution - all futures are called")
    void homeRecommendations_parallelExecution() {
        setupAuthentication();

        // Simulate slow repository calls
        when(recipeRepository.findFeaturedRecipes(any(Pageable.class)))
                .thenAnswer(inv -> {
                    Thread.sleep(50);
                    return new PageImpl<>(mockRecipes);
                });
        when(recipeRepository.findPopularRecipes(any(Pageable.class))).thenAnswer(inv -> {
            Thread.sleep(50);
            return new PageImpl<>(mockRecipes);
        });
        when(recipeRepository.findNewestRecipes(any(Pageable.class))).thenAnswer(inv -> {
            Thread.sleep(50);
            return new PageImpl<>(mockRecipes);
        });
        when(recipeRepository.findTopRatedRecipes(anyInt(), any(Pageable.class))).thenAnswer(inv -> {
            Thread.sleep(50);
            return new PageImpl<>(mockRecipes);
        });
        when(recipeRepository.findTrendingRecipes(any(Pageable.class))).thenAnswer(inv -> {
            Thread.sleep(50);
            return new PageImpl<>(mockRecipes);
        });
        when(recipeRepository.findAllPublishedRecipes()).thenReturn(mockRecipes);

        long start = System.currentTimeMillis();
        recommendationService.getHomeRecommendations();
        long duration = System.currentTimeMillis() - start;

        // Should be around 50-100ms, not 250ms+ if sequential
        assertTrue(duration < 200, "Parallel execution should be faster than sequential");
    }
    @Test
    @DisplayName("Exception in repository propagates as CustomException")
    void repositoryException_propagatesCustomException() {
        setupAuthentication();

        when(recipeRepository.findFeaturedRecipes(any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        CustomException exception = assertThrows(CustomException.class,
                () -> recommendationService.getFeaturedRecipes(10));

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    // Tương tự cho các method khác – chỉ cần 1-2 là đủ để cover pattern
    @Test
    void popularRecipes_repositoryException() {
        setupAuthentication();
        when(recipeRepository.findPopularRecipes(any(Pageable.class)))
                .thenThrow(new RuntimeException("DB error"));

        CustomException ex = assertThrows(CustomException.class,
                () -> recommendationService.getPopularRecipes(10));
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("convertToRecommendationResponses - empty list returns empty")
    void convertToRecommendationResponses_emptyList() throws Exception {
        Method convertMethod = RecommendationServiceImpl.class
                .getDeclaredMethod("convertToRecommendationResponses", List.class);
        convertMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<RecipeRecommendationResponse> result =
                (List<RecipeRecommendationResponse>) convertMethod.invoke(recommendationService, List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("convertToRecommendationResponses - user not found in batch -> Unknown")
    void convertToRecommendationResponses_userNotFound_returnsUnknown() throws Exception {
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of()); // no user

        Method convertMethod = RecommendationServiceImpl.class
                .getDeclaredMethod("convertToRecommendationResponses", List.class);
        convertMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<RecipeRecommendationResponse> result =
                (List<RecipeRecommendationResponse>) convertMethod.invoke(recommendationService, mockRecipes);

        assertEquals("Unknown", result.get(0).getUserName());
    }

    @Test
    @DisplayName("Pagination methods - other sections work")
    void getPopularRecipesWithPagination_success() {
        Page<Recipe> page = new PageImpl<>(mockRecipes, PageRequest.of(1, 10), 25);
        when(recipeRepository.findPopularRecipes(any(Pageable.class))).thenReturn(page);

        RecipeRecommendationPageResponse response =
                recommendationService.getPopularRecipesWithPagination(1, 10);

        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getCurrentPage());
        assertEquals(10, response.getPageSize());
        assertEquals(25, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
    }

    // Thêm 1-2 cái nữa để cover các section khác
    @Test
    void getNewestRecipesWithPagination_success() {
        Page<Recipe> page = new PageImpl<>(mockRecipes);
        when(recipeRepository.findNewestRecipes(any(Pageable.class))).thenReturn(page);

        RecipeRecommendationPageResponse response =
                recommendationService.getNewestRecipesWithPagination(0, 15);

        assertEquals(1, response.getContent().size());
    }

    @Test
    void getTopRatedRecipesWithPagination_success() {
        Page<Recipe> page = new PageImpl<>(mockRecipes);
        when(recipeRepository.findTopRatedRecipes(eq(5), any(Pageable.class))).thenReturn(page);

        RecipeRecommendationPageResponse response =
                recommendationService.getTopRatedRecipesWithPagination(0, 20);

        assertEquals(1, response.getContent().size());
    }

    @Test
    void getTrendingRecipesWithPagination_success() {
        Page<Recipe> page = new PageImpl<>(mockRecipes);
        when(recipeRepository.findTrendingRecipes(any(Pageable.class))).thenReturn(page);

        RecipeRecommendationPageResponse response =
                recommendationService.getTrendingRecipesWithPagination(0, 10);

        assertEquals(1, response.getContent().size());
    }

    // Cover validateLimit message chi tiết (optional nhưng tốt)
    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    void validateLimit_invalid_throwsWithCorrectMessage(int limit) {
        CustomException ex = assertThrows(CustomException.class,
                () -> recommendationService.getFeaturedRecipes(limit));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("1 - 50") || ex.getMessage().contains("1-50"));
    }
    @Test
    @DisplayName("Old createPageResponse - full coverage for legacy method")
    void createPageResponse_legacyMethod_fullCoverage() throws Exception {
        List<Recipe> largeList = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            Recipe r = new Recipe();
            r.setRecipeId(UUID.randomUUID());
            r.setTitle("Recipe " + i);
            r.setUserId(testUser.getUserId());
            r.setFeaturedImage("path/" + i + ".jpg");
            largeList.add(r);
        }

        Method createPageMethod = RecommendationServiceImpl.class
                .getDeclaredMethod("createPageResponse", List.class, int.class, int.class);
        createPageMethod.setAccessible(true);

        // Page 0
        RecipeRecommendationPageResponse response1 = (RecipeRecommendationPageResponse)
                createPageMethod.invoke(recommendationService, largeList, 0, 10);

        assertEquals(10, response1.getContent().size());
        assertEquals(0, response1.getCurrentPage());
        assertEquals(10, response1.getPageSize());
        assertEquals(25, response1.getTotalElements());
        assertEquals(3, response1.getTotalPages());
        assertTrue(response1.isFirst());           // ĐÚNG
        assertFalse(response1.isLast());           // ĐÚNG
        assertTrue(response1.isHasNext());         // ĐÚNG
        assertFalse(response1.isHasPrevious());    // ĐÚNG

        // Page 2 (last page)
        RecipeRecommendationPageResponse response2 = (RecipeRecommendationPageResponse)
                createPageMethod.invoke(recommendationService, largeList, 2, 10);

        assertEquals(5, response2.getContent().size());
        assertFalse(response2.isFirst());
        assertTrue(response2.isLast());
        assertFalse(response2.isHasNext());
        assertTrue(response2.isHasPrevious());

        // Page 5 (out of range)
        RecipeRecommendationPageResponse response3 = (RecipeRecommendationPageResponse)
                createPageMethod.invoke(recommendationService, largeList, 5, 10);

        assertTrue(response3.getContent().isEmpty());
        assertEquals(5, response3.getCurrentPage());
        assertFalse(response3.isFirst());
        assertTrue(response3.isLast());
        assertFalse(response3.isHasNext());
        assertTrue(response3.isHasPrevious());  // vì startIndex >= list.size()
    }

    @Test
    @DisplayName("generateDailySeed - null userId uses only date seed")
    void generateDailySeed_nullUserId() throws Exception {
        Method method = RecommendationServiceImpl.class
                .getDeclaredMethod("generateDailySeed", LocalDate.class, UUID.class);
        method.setAccessible(true);

        LocalDate date = LocalDate.of(2025, 12, 13);

        long seedWithUser = (long) method.invoke(recommendationService, date, testUser.getUserId());
        long seedNoUser = (long) method.invoke(recommendationService, date, null);

        // Phải khác nhau vì nhánh if(userId != null)
        assertNotEquals(seedWithUser, seedNoUser);

        // seedNoUser chỉ dựa vào date → cùng date, cùng seed
        long seedNoUser2 = (long) method.invoke(recommendationService, date, null);
        assertEquals(seedNoUser, seedNoUser2);
    }

    @Test
    @DisplayName("getDailyRecommendations - exception in repository")
    void getDailyRecommendations_repositoryException() {
        when(recipeRepository.findAllPublishedRecipes())
                .thenThrow(new RuntimeException("DB connection failed"));

        CustomException ex = assertThrows(CustomException.class,
                () -> recommendationService.getDailyRecommendations(testUser.getUserId()));

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("validateLimit - logs warn before throwing")
    void validateLimit_logsWarn() {
        // Test limit invalid để trigger log.warn
        CustomException ex = assertThrows(CustomException.class,
                () -> recommendationService.getTrendingRecipes(999));

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());

        // Không verify log trực tiếp (cần PowerMock hoặc Logback test appender)
        // Nhưng việc throw đúng là đủ để cover nhánh warn (vì warn trước throw)
    }

    @Test
    @DisplayName("Pagination edge cases - empty page and last page flags")
    void pagination_edgeCases() {
        Page<Recipe> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(recipeRepository.findNewestRecipes(any(Pageable.class))).thenReturn(emptyPage);

        RecipeRecommendationPageResponse response = recommendationService.getNewestRecipesWithPagination(0, 10);

        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }
}