package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.interaction.dto.response.SearchHistoryResponse;
import com.backend.cookshare.interaction.entity.SearchHistory;
import com.backend.cookshare.interaction.mapper.SearchHistoryMapper;
import com.backend.cookshare.interaction.repository.SearchHistoryRepository;
import com.backend.cookshare.recipe_management.dto.response.IngredientResponse;
import com.backend.cookshare.recipe_management.dto.response.SearchReponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.recipe_management.mapper.SearchMapper;
import com.backend.cookshare.recipe_management.repository.IngredientRepository;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private SearchMapper searchMapper;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SearchHistoryMapper searchHistoryMapper;

    @Mock
    private FirebaseStorageService firebaseStorageService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SearchServiceImpl searchService;

    private Pageable pageable;
    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        userId = UUID.randomUUID();
        testUser = User.builder()
                .userId(userId)
                .username("testuser")
                .fullName("Test User")
                .build();
    }

    private void setupSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
    }

    // ============ searchRecipesByName Tests ============

    @Test
    void searchRecipesByName_WithValidKeyword_ShouldReturnRecipes() {
        String keyword = "pasta";
        Recipe recipe = createTestRecipe();
        Page<Recipe> recipePage = new PageImpl<>(List.of(recipe), pageable, 1);
        SearchReponse searchResponse = createTestSearchResponse();

        setupSecurityContext();
        when(recipeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(recipePage);
        when(searchMapper.toSearchRecipeResponse(recipe)).thenReturn(searchResponse);
        when(firebaseStorageService.convertPathToFirebaseUrl(anyString())).thenReturn("https://firebase.url/image.jpg");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        PageResponse<SearchReponse> result = searchService.searchRecipesByName(keyword, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(recipeRepository).findAll(any(Specification.class), eq(pageable));
        verify(searchHistoryRepository).save(any(SearchHistory.class));
    }

    @Test
    void searchRecipesByName_WithNullKeyword_ShouldThrowException() {
        CustomException exception = assertThrows(CustomException.class,
                () -> searchService.searchRecipesByName(null, pageable));
        assertEquals(ErrorCode.SEARCH_QUERY_EMPTY, exception.getErrorCode());
    }

    @Test
    void searchRecipesByName_WithEmptyKeyword_ShouldThrowException() {
        CustomException exception = assertThrows(CustomException.class,
                () -> searchService.searchRecipesByName("  ", pageable));
        assertEquals(ErrorCode.SEARCH_QUERY_EMPTY, exception.getErrorCode());
    }

    @Test
    void searchRecipesByName_WithTooShortKeyword_ShouldThrowException() {
        CustomException exception = assertThrows(CustomException.class,
                () -> searchService.searchRecipesByName("a", pageable));
        assertEquals(ErrorCode.SEARCH_QUERY_TOO_SHORT, exception.getErrorCode());
    }

    @Test
    void searchRecipesByName_WithTooLongKeyword_ShouldThrowException() {
        String longKeyword = "a".repeat(81);
        CustomException exception = assertThrows(CustomException.class,
                () -> searchService.searchRecipesByName(longKeyword, pageable));
        assertEquals(ErrorCode.SEARCH_QUERY_TOO_LONG, exception.getErrorCode());
    }

    @Test
    void searchRecipesByName_WithInvalidCharacters_ShouldThrowException() {
        CustomException exception = assertThrows(CustomException.class,
                () -> searchService.searchRecipesByName("pasta@#$", pageable));
        assertEquals(ErrorCode.INVALID_CHARACTERS, exception.getErrorCode());
    }

    @Test
    void searchRecipesByName_ShouldSaveSearchHistory() {
        String keyword = "pasta";
        Recipe recipe = createTestRecipe();
        Page<Recipe> recipePage = new PageImpl<>(List.of(recipe), pageable, 1);
        SearchReponse searchResponse = createTestSearchResponse();

        setupSecurityContext();
        when(recipeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(recipePage);
        when(searchMapper.toSearchRecipeResponse(recipe)).thenReturn(searchResponse);
        when(firebaseStorageService.convertPathToFirebaseUrl(anyString())).thenReturn("https://firebase.url/image.jpg");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        searchService.searchRecipesByName(keyword, pageable);

        ArgumentCaptor<SearchHistory> historyCaptor = ArgumentCaptor.forClass(SearchHistory.class);
        verify(searchHistoryRepository).save(historyCaptor.capture());

        SearchHistory savedHistory = historyCaptor.getValue();
        assertEquals(keyword, savedHistory.getSearchQuery());
        assertEquals("recipe", savedHistory.getSearchType());
        assertEquals(1, savedHistory.getResultCount());
        assertEquals(userId, savedHistory.getUserId());
    }

    // ============ searchRecipesByIngredient Tests ============

    @Test
    void searchRecipesByIngredient_WithValidInput_ShouldReturnRecipes() {
        String title = "pasta";
        List<String> ingredients = Arrays.asList("tomato", "cheese");
        Recipe recipe = createTestRecipe();
        Page<Recipe> recipePage = new PageImpl<>(List.of(recipe), pageable, 1);
        SearchReponse searchResponse = createTestSearchResponse();

        setupSecurityContext();
        when(recipeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(recipePage);
        when(searchMapper.toSearchRecipeResponse(recipe)).thenReturn(searchResponse);
        when(firebaseStorageService.convertPathToFirebaseUrl(anyString())).thenReturn("https://firebase.url/image.jpg");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        PageResponse<SearchReponse> result = searchService.searchRecipesByIngredient(title, ingredients, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(recipeRepository).findAll(any(Specification.class), eq(pageable));
        verify(searchHistoryRepository).save(any(SearchHistory.class));
    }

    @Test
    void searchRecipesByIngredient_WithNullTitle_ShouldThrowException() {
        List<String> ingredients = Arrays.asList("tomato", "cheese");

        CustomException exception = assertThrows(CustomException.class,
                () -> searchService.searchRecipesByIngredient(null, ingredients, pageable));
        assertEquals(ErrorCode.SEARCH_QUERY_EMPTY, exception.getErrorCode());
    }

    @Test
    void searchRecipesByIngredient_WithEmptyResults_ShouldNotSaveHistory() {
        String title = "pasta";
        List<String> ingredients = Arrays.asList("tomato", "cheese");
        Page<Recipe> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(recipeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        PageResponse<SearchReponse> result = searchService.searchRecipesByIngredient(title, ingredients, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(searchHistoryRepository, never()).save(any(SearchHistory.class));
    }

    // ============ top10MostUsedIngredients Tests ============

    @Test
    void top10MostUsedIngredients_ShouldReturnTopIngredients() {
        List<Object[]> ingredientData = Arrays.asList(
                new Object[]{"Tomato", 100L},
                new Object[]{"Cheese", 80L}
        );
        IngredientResponse response1 = IngredientResponse.builder()
                .name("Tomato")
                .build();
        IngredientResponse response2 = IngredientResponse.builder()
                .name("Cheese")
                .build();

        when(ingredientRepository.findTop10MostUsedIngredients()).thenReturn(ingredientData);
        when(searchMapper.toIngredientResponseFromArray(ingredientData.get(0))).thenReturn(response1);
        when(searchMapper.toIngredientResponseFromArray(ingredientData.get(1))).thenReturn(response2);

        List<IngredientResponse> result = searchService.top10MostUsedIngredients();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Tomato", result.get(0).getName());
        verify(ingredientRepository).findTop10MostUsedIngredients();
    }

    // ============ getSearchHistory Tests ============

    @Test
    void getSearchHistory_ShouldReturnUserSearchHistory() {
        SearchHistory history1 = SearchHistory.builder()
                .userId(userId)
                .searchQuery("pasta")
                .searchType("recipe")
                .resultCount(5)
                .build();

        SearchHistoryResponse response1 = SearchHistoryResponse.builder()
                .searchQuery("pasta")
                .searchType("recipe")
                .resultCount(5)
                .build();

        setupSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(searchHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(history1));
        when(searchHistoryMapper.toSearchHistoryResponse(history1)).thenReturn(response1);

        List<SearchHistoryResponse> result = searchService.getSearchHistory();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("pasta", result.get(0).getSearchQuery());
        verify(searchHistoryRepository).findTop5ByUserIdOrderByCreatedAtDesc(userId);
    }

    // ============ searchRecipesByfullName Tests ============

    @Test
    void searchRecipesByfullName_WithRecipeFound_ShouldReturnRecipes() {
        String keyword = "pasta";
        Recipe recipe = createTestRecipe();
        Page<Recipe> recipePage = new PageImpl<>(List.of(recipe), pageable, 1);
        SearchReponse searchResponse = createTestSearchResponse();

        when(recipeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(recipePage);
        when(searchMapper.toSearchRecipeResponse(recipe)).thenReturn(searchResponse);
        when(firebaseStorageService.convertPathToFirebaseUrl(anyString())).thenReturn("https://firebase.url/image.jpg");

        PageResponse<SearchReponse> result = searchService.searchRecipesByfullName(keyword, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(recipeRepository).findAll(any(Specification.class), eq(pageable));
        verify(userRepository, never()).findByFullNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    void searchRecipesByfullName_WithNoRecipeButUserFound_ShouldReturnUsers() {
        String keyword = "John";
        Page<Recipe> emptyRecipePage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
        SearchReponse userResponse = SearchReponse.builder()
                .fullName("Test User")
                .build();

        when(recipeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyRecipePage);
        when(userRepository.findByFullNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
                .thenReturn(userPage);
        when(searchMapper.toSearchUserResponse(testUser)).thenReturn(userResponse);

        PageResponse<SearchReponse> result = searchService.searchRecipesByfullName(keyword, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test User", result.getContent().get(0).getFullName());
        verify(userRepository).findByFullNameContainingIgnoreCase(eq(keyword), any(Pageable.class));
    }

    @Test
    void searchRecipesByfullName_WithNoRecipeAndNoUser_ShouldThrowException() {
        String keyword = "nonexistent";
        Page<Recipe> emptyRecipePage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        Page<User> emptyUserPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(recipeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyRecipePage);
        when(userRepository.findByFullNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
                .thenReturn(emptyUserPage);

        CustomException exception = assertThrows(CustomException.class,
                () -> searchService.searchRecipesByfullName(keyword, pageable));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    // ============ getUsernameSuggestions Tests ============

    @Test
    void getUsernameSuggestions_ShouldReturnUsernameSuggestions() {
        String query = "test";
        int limit = 5;
        List<User> users = Arrays.asList(
                User.builder().fullName("Test User 1").build(),
                User.builder().fullName("Test User 2").build()
        );

        when(userRepository.findByFullNameContainingIgnoreCase(query.toLowerCase()))
                .thenReturn(users);

        List<String> result = searchService.getUsernameSuggestions(query, limit);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Test User 1"));
        assertTrue(result.contains("Test User 2"));
    }

    @Test
    void getUsernameSuggestions_WithLimit_ShouldRespectLimit() {
        String query = "test";
        int limit = 1;
        List<User> users = Arrays.asList(
                User.builder().fullName("Test User 1").build(),
                User.builder().fullName("Test User 2").build(),
                User.builder().fullName("Test User 3").build()
        );

        when(userRepository.findByFullNameContainingIgnoreCase(query.toLowerCase()))
                .thenReturn(users);

        List<String> result = searchService.getUsernameSuggestions(query, limit);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ============ getRecipeSuggestions Tests ============

    @Test
    void getRecipeSuggestions_ShouldReturnRecipeSuggestions() {
        String query = "pasta";
        int limit = 5;
        List<Recipe> recipes = Arrays.asList(
                Recipe.builder().title("Pasta Carbonara").status(RecipeStatus.APPROVED).build(),
                Recipe.builder().title("Pasta Bolognese").status(RecipeStatus.APPROVED).build()
        );

        when(recipeRepository.findByTitleContainingIgnoreCaseAndStatus(
                query.toLowerCase(), RecipeStatus.APPROVED))
                .thenReturn(recipes);

        List<String> result = searchService.getRecipeSuggestions(query, limit);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Pasta Carbonara"));
        assertTrue(result.contains("Pasta Bolognese"));
    }

    @Test
    void getRecipeSuggestions_WithLimit_ShouldRespectLimit() {
        String query = "pasta";
        int limit = 1;
        List<Recipe> recipes = Arrays.asList(
                Recipe.builder().title("Pasta Carbonara").status(RecipeStatus.APPROVED).build(),
                Recipe.builder().title("Pasta Bolognese").status(RecipeStatus.APPROVED).build(),
                Recipe.builder().title("Pasta Primavera").status(RecipeStatus.APPROVED).build()
        );

        when(recipeRepository.findByTitleContainingIgnoreCaseAndStatus(
                query.toLowerCase(), RecipeStatus.APPROVED))
                .thenReturn(recipes);

        List<String> result = searchService.getRecipeSuggestions(query, limit);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getRecipeSuggestions_ShouldReturnDistinctTitles() {
        String query = "pasta";
        int limit = 5;
        List<Recipe> recipes = Arrays.asList(
                Recipe.builder().title("Pasta Carbonara").status(RecipeStatus.APPROVED).build(),
                Recipe.builder().title("Pasta Carbonara").status(RecipeStatus.APPROVED).build(),
                Recipe.builder().title("Pasta Bolognese").status(RecipeStatus.APPROVED).build()
        );

        when(recipeRepository.findByTitleContainingIgnoreCaseAndStatus(
                query.toLowerCase(), RecipeStatus.APPROVED))
                .thenReturn(recipes);

        List<String> result = searchService.getRecipeSuggestions(query, limit);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Pasta Carbonara"));
        assertTrue(result.contains("Pasta Bolognese"));
    }

    // ============ Helper Methods ============

    private Recipe createTestRecipe() {
        return Recipe.builder()
                .recipeId(UUID.randomUUID())
                .title("Test Recipe")
                .featuredImage("recipes/test.jpg")
                .status(RecipeStatus.APPROVED)
                .build();
    }

    private SearchReponse createTestSearchResponse() {
        return SearchReponse.builder()
                .recipeId(UUID.randomUUID())
                .title("Test Recipe")
                .featuredImage("recipes/test.jpg")
                .build();
    }
}