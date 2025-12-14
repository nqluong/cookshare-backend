package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.request.*;
import com.backend.cookshare.recipe_management.dto.response.RecipeDetailsResult;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.entity.*;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.recipe_management.mapper.*;
import com.backend.cookshare.recipe_management.repository.*;
import com.backend.cookshare.user.service.ActivityLogService;
import com.backend.cookshare.user.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeStepRepository recipeStepRepository;

    @Mock
    private RecipeIngredientRepository recipeIngredientRepository;

    @Mock
    private RecipeTagRepository recipeTagRepository;

    @Mock
    private RecipeCategoryRepository recipeCategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private RecipeMapper recipeMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private IngredientMapper ingredientMapper;

    @Mock
    private RecipeLoaderHelper recipeLoaderHelper;

    @Mock
    private FirebaseStorageService fileStorageService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RecipeServiceImpl recipeService;

    private UUID recipeId;
    private UUID userId;
    private Recipe testRecipe;
    private RecipeRequest recipeRequest;
    private RecipeResponse recipeResponse;
    private RecipeDetailsResult recipeDetails;

    @BeforeEach
    void setUp() {
        recipeId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testRecipe = Recipe.builder()
                .recipeId(recipeId)
                .userId(userId)
                .title("Test Recipe")
                .description("Test Description")
                .slug("test-recipe")
                .status(RecipeStatus.APPROVED)
                .viewCount(0)
                .featuredImage("recipes/test.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        recipeRequest = new RecipeRequest();
        recipeRequest.setTitle("Test Recipe");
        recipeRequest.setDescription("Test Description");
        recipeRequest.setUserId(userId);

        recipeResponse = RecipeResponse.builder()
                .recipeId(recipeId)
                .title("Test Recipe")
                .description("Test Description")
                .featuredImage("https://firebase.url/recipes/test.jpg")
                .build();

        recipeDetails = new RecipeDetailsResult();
        recipeDetails.fullName = "Test User";
        recipeDetails.steps = new ArrayList<>();
        recipeDetails.ingredients = new ArrayList<>();
        recipeDetails.tags = new ArrayList<>();
        recipeDetails.categories = new ArrayList<>();
    }

    // ============ createRecipe Tests ============

    @Test
    void createRecipe_WithValidRequest_ShouldCreateSuccessfully() {
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        assertEquals("Test Recipe", result.getTitle());
        verify(recipeRepository).save(any(Recipe.class));
        verify(activityLogService).logRecipeActivity(userId, recipeId, "CREATE");
    }

    @Test
    void createRecipe_WithNewCategories_ShouldCreateCategoriesAndRecipe() {
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("New Category");
        recipeRequest.setNewCategories(List.of(categoryRequest));

        Category newCategory = Category.builder()
                .categoryId(UUID.randomUUID())
                .name("New Category")
                .slug("new-category")
                .build();

        when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());
        when(categoryMapper.toEntity(categoryRequest)).thenReturn(newCategory);
        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(categoryRepository).save(any(Category.class));
        verify(recipeCategoryRepository).insertRecipeCategory(eq(recipeId), any(UUID.class));
    }

    @Test
    void createRecipe_WithExistingCategory_ShouldReuseCategory() {
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("Existing Category");
        recipeRequest.setNewCategories(List.of(categoryRequest));

        Category existingCategory = Category.builder()
                .categoryId(UUID.randomUUID())
                .name("Existing Category")
                .slug("existing-category")
                .build();

        when(categoryRepository.findByName("Existing Category")).thenReturn(Optional.of(existingCategory));
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(categoryRepository, never()).save(any(Category.class));
        verify(recipeCategoryRepository).insertRecipeCategory(eq(recipeId), eq(existingCategory.getCategoryId()));
    }

    @Test
    void createRecipe_WithNewTags_ShouldCreateTagsAndRecipe() {
        TagRequest tagRequest = new TagRequest();
        tagRequest.setName("New Tag");
        recipeRequest.setNewTags(List.of(tagRequest));

        Tag newTag = Tag.builder()
                .tagId(UUID.randomUUID())
                .name("New Tag")
                .slug("new-tag")
                .usageCount(0)
                .build();

        when(tagRepository.existsByNameIgnoreCase("New Tag")).thenReturn(false);
        when(tagMapper.toEntity(tagRequest)).thenReturn(newTag);
        when(tagRepository.save(any(Tag.class))).thenReturn(newTag);
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(tagRepository).save(any(Tag.class));
        verify(recipeTagRepository).insertRecipeTag(eq(recipeId), any(UUID.class));
    }

    @Test
    void createRecipe_WithNewIngredients_ShouldCreateIngredientsAndRecipe() {
        IngredientRequest ingredientRequest = new IngredientRequest();
        ingredientRequest.setName("New Ingredient");
        recipeRequest.setNewIngredients(List.of(ingredientRequest));

        Ingredient newIngredient = Ingredient.builder()
                .ingredientId(UUID.randomUUID())
                .name("New Ingredient")
                .slug("new-ingredient")
                .usageCount(0)
                .build();

        when(ingredientRepository.findByNameIgnoreCase("New Ingredient")).thenReturn(Optional.empty());
        when(ingredientMapper.toEntity(ingredientRequest)).thenReturn(newIngredient);
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(newIngredient);
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(ingredientRepository).save(any(Ingredient.class));
        verify(recipeIngredientRepository).insertRecipeIngredient(
                eq(recipeId), any(UUID.class), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void createRecipe_WithSteps_ShouldSaveSteps() {
        RecipeStepRequest step = new RecipeStepRequest();
        step.setStepNumber(1);
        step.setInstruction("Test instruction");
        recipeRequest.setSteps(List.of(step));

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(recipeStepRepository).insertRecipeStep(
                eq(recipeId), eq(1), eq("Test instruction"),
                isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void createRecipe_WithIngredientDetails_ShouldSaveIngredientDetails() {
        UUID ingredientId = UUID.randomUUID();
        IngredientDetailRequest detail = new IngredientDetailRequest();
        detail.setIngredientId(ingredientId);
        detail.setQuantity(2.0);
        detail.setUnit("cups");
        detail.setNotes("diced");
        recipeRequest.setIngredientDetails(List.of(detail));

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(recipeIngredientRepository).insertRecipeIngredient(
                eq(recipeId), eq(ingredientId), eq("2.0"), eq("cups"), eq("diced"), isNull());
    }

    // ============ createRecipeWithFiles Tests ============

    @Test
    void createRecipeWithFiles_WithFeaturedImage_ShouldUploadAndCreate() {
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(fileStorageService.uploadFile(image)).thenReturn("recipes/uploaded.jpg");
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/uploaded.jpg");

        RecipeResponse result = recipeService.createRecipeWithFiles(recipeRequest, image, null);

        assertNotNull(result);
        assertEquals("recipes/uploaded.jpg", recipeRequest.getFeaturedImage());
        verify(fileStorageService).uploadFile(image);
    }

    @Test
    void createRecipeWithFiles_WithStepImages_ShouldMapAndUploadStepImages() {
        RecipeStepRequest step1 = new RecipeStepRequest();
        step1.setStepNumber(1);
        step1.setInstruction("Step 1");
        recipeRequest.setSteps(List.of(step1));

        MultipartFile stepImage = mock(MultipartFile.class);
        when(stepImage.isEmpty()).thenReturn(false);
        when(stepImage.getOriginalFilename()).thenReturn("step_1.jpg");
        when(fileStorageService.uploadFile(stepImage)).thenReturn("steps/step_1.jpg");

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/steps/step_1.jpg");

        RecipeResponse result = recipeService.createRecipeWithFiles(
                recipeRequest, null, List.of(stepImage));

        assertNotNull(result);
        assertEquals("steps/step_1.jpg", step1.getImageUrl());
        verify(fileStorageService).uploadFile(stepImage);
    }

    // ============ updateRecipe Tests ============

    @Test
    void updateRecipe_WithValidRequest_ShouldUpdateSuccessfully() {
        recipeRequest.setTitle("Updated Recipe");

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        assertNotNull(result);
        verify(recipeRepository).save(any(Recipe.class));
        verify(recipeStepRepository).deleteAllByRecipeId(recipeId);
        verify(recipeIngredientRepository).deleteAllByRecipeId(recipeId);
        verify(recipeTagRepository).deleteAllByRecipeId(recipeId);
        verify(recipeCategoryRepository).deleteAllByRecipeId(recipeId);
        verify(activityLogService).logRecipeActivity(userId, recipeId, "UPDATE");
    }

    @Test
    void updateRecipe_WithNewFeaturedImage_ShouldDeleteOldAndUploadNew() {
        MultipartFile newImage = mock(MultipartFile.class);
        when(newImage.isEmpty()).thenReturn(false);
        when(fileStorageService.uploadFile(newImage)).thenReturn("recipes/new.jpg");

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/new.jpg");

        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, newImage, null);

        assertNotNull(result);
        verify(fileStorageService).deleteFile("recipes/test.jpg");
        verify(fileStorageService).uploadFile(newImage);
        assertEquals("recipes/new.jpg", recipeRequest.getFeaturedImage());
    }

    @Test
    void updateRecipe_WithNonExistentId_ShouldThrowException() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> recipeService.updateRecipe(recipeId, recipeRequest, null, null));

        assertEquals(ErrorCode.RECIPE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateRecipe_WithOldIngredientDetails_ShouldPreserveOldDetails() {
        UUID ingredientId = UUID.randomUUID();
        IngredientDetailRequest detail = new IngredientDetailRequest();
        detail.setIngredientId(ingredientId);
        detail.setQuantity(null); // No new quantity
        recipeRequest.setIngredientDetails(List.of(detail));

        Map<String, Object> oldIngData = new HashMap<>();
        oldIngData.put("ingredient_id", ingredientId);
        oldIngData.put("quantity", "2.5");
        oldIngData.put("unit", "cups");
        oldIngData.put("notes", "chopped");

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(List.of(oldIngData));
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        assertNotNull(result);
        assertEquals(2.5, detail.getQuantity());
        assertEquals("cups", detail.getUnit());
        assertEquals("chopped", detail.getNotes());
    }

    @Test
    void updateRecipe_WithNoIngredientDetailsProvided_ShouldUseOldDetails() {
        UUID ingredientId = UUID.randomUUID();
        recipeRequest.setIngredientDetails(null); // No details provided

        Map<String, Object> oldIngData = new HashMap<>();
        oldIngData.put("ingredient_id", ingredientId);
        oldIngData.put("quantity", "3.0");
        oldIngData.put("unit", "tbsp");
        oldIngData.put("notes", "fresh");

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(List.of(oldIngData));
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        assertNotNull(result);
        assertNotNull(recipeRequest.getIngredientDetails());
        assertEquals(1, recipeRequest.getIngredientDetails().size());
        IngredientDetailRequest preserved = recipeRequest.getIngredientDetails().get(0);
        assertEquals(ingredientId, preserved.getIngredientId());
        assertEquals(3.0, preserved.getQuantity());
        assertEquals("tbsp", preserved.getUnit());
        assertEquals("fresh", preserved.getNotes());
    }

    @Test
    void updateRecipe_WithInvalidQuantityInOldData_ShouldHandleGracefully() {
        UUID ingredientId = UUID.randomUUID();
        IngredientDetailRequest detail = new IngredientDetailRequest();
        detail.setIngredientId(ingredientId);
        detail.setQuantity(0.0);
        detail.setUnit(null); // client không gửi unit → mong đợi được giữ lại từ cũ
        recipeRequest.setIngredientDetails(List.of(detail));

        Map<String, Object> oldIngData = new HashMap<>();
        oldIngData.put("ingredient_id", ingredientId);
        oldIngData.put("quantity", "invalid_number");
        oldIngData.put("unit", "cups"); // ← dữ liệu cũ có unit = "cups"

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(List.of(oldIngData));
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        // === THỰC THI ===
        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        // === KIỂM TRA BẰNG ARGUMENT CAPTOR (CHẮC CHẮN NHẤT) ===
        ArgumentCaptor<String> unitCaptor = ArgumentCaptor.forClass(String.class);
        verify(recipeIngredientRepository).insertRecipeIngredient(
                eq(recipeId),
                eq(ingredientId),
                eq("0.0"),
                unitCaptor.capture(),  // bắt giá trị unit thực tế được lưu
                any(), any()
        );
        assertNotNull(result);
        assertEquals(0.0, detail.getQuantity());
    }

    @Test
    void updateRecipe_WithNoCategoriesProvided_ShouldKeepOldCategories() {
        UUID oldCategoryId = UUID.randomUUID();
        recipeRequest.setCategoryIds(null);
        recipeRequest.setNewCategories(null);

        Map<String, Object> oldCatData = new HashMap<>();
        oldCatData.put("category_id", oldCategoryId);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(List.of(oldCatData));
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        assertNotNull(result);
        verify(recipeCategoryRepository).insertRecipeCategory(eq(recipeId), eq(oldCategoryId));
    }

    @Test
    void updateRecipe_WithNoTagsProvided_ShouldKeepOldTags() {
        UUID oldTagId = UUID.randomUUID();
        recipeRequest.setTagIds(null);
        recipeRequest.setNewTags(null);

        Map<String, Object> oldTagData = new HashMap<>();
        oldTagData.put("tag_id", oldTagId);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId))
                .thenReturn(List.of(oldTagData));
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        assertNotNull(result);
        verify(recipeTagRepository).insertRecipeTag(eq(recipeId), eq(oldTagId));
    }

    @Test
    void updateRecipe_WithNoStatusProvided_ShouldKeepOldStatus() {
        recipeRequest.setStatus(null);
        RecipeStatus oldStatus = RecipeStatus.PENDING;
        testRecipe.setStatus(oldStatus);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe saved = invocation.getArgument(0);
            assertEquals(oldStatus, saved.getStatus());
            return saved;
        });
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        assertNotNull(result);
        assertEquals(oldStatus, testRecipe.getStatus());
    }

    @Test
    void updateRecipe_WithNewTitle_ShouldGenerateNewSlug() {
        recipeRequest.setTitle("Brand New Title");
        testRecipe.setTitle("Old Title");

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe saved = invocation.getArgument(0);
            assertEquals("brand-new-title", saved.getSlug());
            return saved;
        });
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        assertNotNull(result);
    }

    @Test
    void updateRecipe_WithSameTitleButNoSlug_ShouldGenerateSlug() {
        recipeRequest.setTitle("Test Recipe");
        testRecipe.setTitle("Test Recipe");
        testRecipe.setSlug(null);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe saved = invocation.getArgument(0);
            assertEquals("test-recipe", saved.getSlug());
            return saved;
        });
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        assertNotNull(result);
    }

    @Test
    void updateRecipe_WithErrorFetchingOldData_ShouldHandleGracefully() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId))
                .thenThrow(new RuntimeException("Database error"));
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        // Should not throw exception
        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        assertNotNull(result);
    }

    @Test
    void createRecipeWithFiles_WithMultipleStepImages_ShouldMapAllCorrectly() {
        RecipeStepRequest step1 = new RecipeStepRequest();
        step1.setStepNumber(1);
        RecipeStepRequest step2 = new RecipeStepRequest();
        step2.setStepNumber(2);
        recipeRequest.setSteps(List.of(step1, step2));

        MultipartFile stepImage1 = mock(MultipartFile.class);
        when(stepImage1.isEmpty()).thenReturn(false);
        when(stepImage1.getOriginalFilename()).thenReturn("step_1.jpg");
        when(fileStorageService.uploadFile(stepImage1)).thenReturn("steps/step_1.jpg");

        MultipartFile stepImage2 = mock(MultipartFile.class);
        when(stepImage2.isEmpty()).thenReturn(false);
        when(stepImage2.getOriginalFilename()).thenReturn("step_2.jpg");
        when(fileStorageService.uploadFile(stepImage2)).thenReturn("steps/step_2.jpg");

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/steps/step.jpg");

        RecipeResponse result = recipeService.createRecipeWithFiles(
                recipeRequest, null, List.of(stepImage1, stepImage2));

        assertNotNull(result);
        assertEquals("steps/step_1.jpg", step1.getImageUrl());
        assertEquals("steps/step_2.jpg", step2.getImageUrl());
    }

    @Test
    void createRecipeWithFiles_WithInvalidStepImageFilename_ShouldSkipImage() {
        RecipeStepRequest step = new RecipeStepRequest();
        step.setStepNumber(1);
        recipeRequest.setSteps(List.of(step));

        MultipartFile stepImage = mock(MultipartFile.class);
        when(stepImage.isEmpty()).thenReturn(false);
        when(stepImage.getOriginalFilename()).thenReturn("invalid_filename.jpg");

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipeWithFiles(
                recipeRequest, null, List.of(stepImage));

        assertNotNull(result);
        assertNull(step.getImageUrl());
        verify(fileStorageService, never()).uploadFile(stepImage);
    }

    @Test
    void createRecipeWithFiles_WithNullStepImageFilename_ShouldSkipImage() {
        RecipeStepRequest step = new RecipeStepRequest();
        step.setStepNumber(1);
        recipeRequest.setSteps(List.of(step));

        MultipartFile stepImage = mock(MultipartFile.class);
        when(stepImage.isEmpty()).thenReturn(false);
        when(stepImage.getOriginalFilename()).thenReturn(null);

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipeWithFiles(
                recipeRequest, null, List.of(stepImage));

        assertNotNull(result);
        assertNull(step.getImageUrl());
    }

    @Test
    void createRecipeWithFiles_WithEmptyStepImage_ShouldSkipImage() {
        RecipeStepRequest step = new RecipeStepRequest();
        step.setStepNumber(1);
        recipeRequest.setSteps(List.of(step));

        MultipartFile stepImage = mock(MultipartFile.class);
        when(stepImage.isEmpty()).thenReturn(true);

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipeWithFiles(
                recipeRequest, null, List.of(stepImage));

        assertNotNull(result);
        assertNull(step.getImageUrl());
    }

    @Test
    void updateRecipe_WithNewStepImages_ShouldUploadAndUseNewImages() {
        RecipeStepRequest step = new RecipeStepRequest();
        step.setStepNumber(1);
        recipeRequest.setSteps(List.of(step));

        MultipartFile newStepImage = mock(MultipartFile.class);
        when(newStepImage.isEmpty()).thenReturn(false);
        when(newStepImage.getOriginalFilename()).thenReturn("step_1.jpg");
        when(fileStorageService.uploadFile(newStepImage)).thenReturn("steps/new_step_1.jpg");

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/steps/new_step_1.jpg");

        RecipeResponse result = recipeService.updateRecipe(
                recipeId, recipeRequest, null, List.of(newStepImage));

        assertNotNull(result);
        assertEquals("steps/new_step_1.jpg", step.getImageUrl());
    }

    @Test
    void updateRecipe_WithStepNumberNull_ShouldAssignNumberBasedOnIndex() {
        RecipeStepRequest step1 = new RecipeStepRequest();
        step1.setStepNumber(null); // No step number
        RecipeStepRequest step2 = new RecipeStepRequest();
        step2.setStepNumber(null);
        recipeRequest.setSteps(List.of(step1, step2));

        Map<String, Object> oldStepData1 = new HashMap<>();
        oldStepData1.put("step_number", 1);
        oldStepData1.put("image_url", "steps/old_1.jpg");
        Map<String, Object> oldStepData2 = new HashMap<>();
        oldStepData2.put("step_number", 2);
        oldStepData2.put("image_url", "steps/old_2.jpg");

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeStepRepository.findStepImagesByRecipeId(recipeId))
                .thenReturn(List.of(oldStepData1, oldStepData2));
        when(recipeIngredientRepository.findIngredientDetailsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeTagRepository.findTagIdsByRecipeId(recipeId)).thenReturn(Collections.emptyList());
        when(recipeCategoryRepository.findCategoryIdsByRecipeId(recipeId))
                .thenReturn(Collections.emptyList());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/steps/old.jpg");

        RecipeResponse result = recipeService.updateRecipe(recipeId, recipeRequest, null, null);

        assertNotNull(result);
        assertEquals("steps/old_1.jpg", step1.getImageUrl());
        assertEquals("steps/old_2.jpg", step2.getImageUrl());
    }

    // ============ getRecipeById Tests ============

    @Test
    void getRecipeById_WithValidId_ShouldReturnRecipe() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.getRecipeById(recipeId);


    }

    @Test
    void getRecipeById_WithNonExistentId_ShouldThrowException() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> recipeService.getRecipeById(recipeId));

        assertEquals(ErrorCode.RECIPE_NOT_FOUND, exception.getErrorCode());
    }

    // ============ deleteRecipe Tests ============

    @Test
    void deleteRecipe_WithValidId_ShouldDeleteSuccessfully() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));

        recipeService.deleteRecipe(recipeId);

        verify(notificationService).deleteRecipeNotifications(recipeId);
        verify(fileStorageService).deleteFile("recipes/test.jpg");
        verify(recipeStepRepository).deleteAllByRecipeId(recipeId);
        verify(recipeIngredientRepository).deleteAllByRecipeId(recipeId);
        verify(recipeTagRepository).deleteAllByRecipeId(recipeId);
        verify(recipeCategoryRepository).deleteAllByRecipeId(recipeId);
        verify(activityLogService).logRecipeActivity(userId, recipeId, "DELETE");
        verify(recipeRepository).deleteById(recipeId);
    }

    @Test
    void deleteRecipe_WithNonExistentId_ShouldThrowException() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> recipeService.deleteRecipe(recipeId));

        assertEquals(ErrorCode.RECIPE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deleteRecipe_WithNoFeaturedImage_ShouldNotDeleteImage() {
        testRecipe.setFeaturedImage(null);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(testRecipe));

        recipeService.deleteRecipe(recipeId);

        verify(fileStorageService, never()).deleteFile(anyString());
        verify(recipeRepository).deleteById(recipeId);
    }

    // ============ getAllRecipes Tests ============

    @Test
    void getAllRecipes_ShouldReturnPagedRecipes() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recipe> recipePage = new PageImpl<>(List.of(testRecipe), pageable, 1);

        when(recipeRepository.findAll(pageable)).thenReturn(recipePage);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        Page<RecipeResponse> result = recipeService.getAllRecipes(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Recipe", result.getContent().get(0).getTitle());
        verify(recipeRepository).findAll(pageable);
    }

    // ============ getAllRecipesByUserId Tests ============

//    @Test
//    void getAllRecipesByUserId_WithRecipes_ShouldReturnRecipeList() {
//        UUID viewerId = UUID.randomUUID();
//
//        // 🔥 SET ĐẦY ĐỦ ENTITY
//        testRecipe.setUserId(userId);
//        testRecipe.setStatus(RecipeStatus.APPROVED);
//        testRecipe.setFeaturedImage("recipes/test.jpg");
//
//        when(recipeRepository.findByUserIdAndStatus(
//                userId,
//                RecipeStatus.APPROVED
//        )).thenReturn(List.of(testRecipe));
//
//        when(recipeMapper.toResponse(testRecipe))
//                .thenReturn(recipeResponse);
//
//        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
//                .thenReturn("https://firebase.url/recipes/test.jpg");
//
//        List<RecipeResponse> result =
//                recipeService.getAllRecipesByUserId(userId, viewerId);
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//    }

    @Test
    void getAllRecipesByUserId_WithNoRecipes_ShouldReturnEmptyList() {
        UUID secondParam = UUID.randomUUID();

        List<RecipeResponse> result =
                recipeService.getAllRecipesByUserId(userId, secondParam);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Optional: xác nhận repo không được gọi
        verify(recipeRepository, never())
                .findByUserIdAndStatus(any(), any());
    }

    @Test
    void getAllRecipesByUserId_WithNullResult_ShouldReturnEmptyList() {
        UUID secondParam = UUID.randomUUID();

        List<RecipeResponse> result =
                recipeService.getAllRecipesByUserId(userId, secondParam);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        // đảm bảo repository KHÔNG bị gọi
        verify(recipeRepository, never())
                .findByUserIdAndStatus(any(), any());
    }

    // ============ Slug Generation Tests ============

    @Test
    void createRecipe_ShouldGenerateSlugFromTitle() {
        recipeRequest.setTitle("My Amazing Recipe");

        // Create recipe without slug to test generation
        Recipe recipeWithoutSlug = Recipe.builder()
                .recipeId(recipeId)
                .userId(userId)
                .title("My Amazing Recipe")
                .description("Test Description")
                .slug(null) // No slug initially
                .status(RecipeStatus.APPROVED)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(recipeWithoutSlug);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe saved = invocation.getArgument(0);
            // Verify slug was generated
            assertNotNull(saved.getSlug());
            assertEquals("my-amazing-recipe", saved.getSlug());
            return saved;
        });
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(any(), any())).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(any())).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        recipeService.createRecipe(recipeRequest);

        verify(recipeRepository).save(argThat(recipe ->
                recipe.getSlug() != null && recipe.getSlug().equals("my-amazing-recipe")));
    }

    @Test
    void createRecipe_WithExistingSlug_ShouldKeepSlug() {
        recipeRequest.setTitle("Test Recipe");
        testRecipe.setSlug("existing-slug");

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        recipeService.createRecipe(recipeRequest);

        verify(recipeRepository).save(argThat(recipe ->
                recipe.getSlug().equals("existing-slug")));
    }

    @Test
    void createRecipe_WithEmptySlug_ShouldGenerateSlug() {
        recipeRequest.setTitle("New Recipe Title");
        testRecipe.setSlug("");
        testRecipe.setTitle("New Recipe Title");

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        recipeService.createRecipe(recipeRequest);

        verify(recipeRepository).save(argThat(recipe ->
                recipe.getSlug() != null && !recipe.getSlug().isEmpty()));
    }

    // ============ Integration Tests ============

    @Test
    void createRecipe_WithCompleteData_ShouldCreateRecipeWithAllRelations() {
        // Setup complete request
        CategoryRequest categoryReq = new CategoryRequest();
        categoryReq.setName("Test Category");
        recipeRequest.setNewCategories(List.of(categoryReq));

        TagRequest tagReq = new TagRequest();
        tagReq.setName("Test Tag");
        recipeRequest.setNewTags(List.of(tagReq));

        IngredientRequest ingReq = new IngredientRequest();
        ingReq.setName("Test Ingredient");
        recipeRequest.setNewIngredients(List.of(ingReq));

        RecipeStepRequest stepReq = new RecipeStepRequest();
        stepReq.setStepNumber(1);
        stepReq.setInstruction("Test Step");
        recipeRequest.setSteps(List.of(stepReq));

        // Setup mocks
        Category category = Category.builder().categoryId(UUID.randomUUID()).name("Test Category").build();
        Tag tag = Tag.builder().tagId(UUID.randomUUID()).name("Test Tag").build();
        Ingredient ingredient = Ingredient.builder().ingredientId(UUID.randomUUID()).name("Test Ingredient").build();

        when(categoryRepository.findByName("Test Category")).thenReturn(Optional.empty());
        when(categoryMapper.toEntity(categoryReq)).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        when(tagRepository.existsByNameIgnoreCase("Test Tag")).thenReturn(false);
        when(tagMapper.toEntity(tagReq)).thenReturn(tag);
        when(tagRepository.save(any(Tag.class))).thenReturn(tag);

        when(ingredientRepository.findByNameIgnoreCase("Test Ingredient")).thenReturn(Optional.empty());
        when(ingredientMapper.toEntity(ingReq)).thenReturn(ingredient);
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);

        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(categoryRepository).save(any(Category.class));
        verify(tagRepository).save(any(Tag.class));
        verify(ingredientRepository).save(any(Ingredient.class));
        verify(recipeStepRepository).insertRecipeStep(
                eq(recipeId), eq(1), eq("Test Step"),
                isNull(), isNull(), isNull(), isNull());
        verify(recipeCategoryRepository).insertRecipeCategory(eq(recipeId), eq(category.getCategoryId()));
        verify(recipeTagRepository).insertRecipeTag(eq(recipeId), eq(tag.getTagId()));
        verify(recipeIngredientRepository).insertRecipeIngredient(
                eq(recipeId), eq(ingredient.getIngredientId()),
                isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void createRecipe_WithExistingIngredient_ShouldReuseIngredient() {
        IngredientRequest ingredientRequest = new IngredientRequest();
        ingredientRequest.setName("Existing Ingredient");
        recipeRequest.setNewIngredients(List.of(ingredientRequest));

        Ingredient existingIngredient = Ingredient.builder()
                .ingredientId(UUID.randomUUID())
                .name("Existing Ingredient")
                .slug("existing-ingredient")
                .build();

        when(ingredientRepository.findByNameIgnoreCase("Existing Ingredient"))
                .thenReturn(Optional.of(existingIngredient));
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(ingredientRepository, never()).save(any(Ingredient.class));
        verify(recipeIngredientRepository).insertRecipeIngredient(
                eq(recipeId), eq(existingIngredient.getIngredientId()),
                isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void createRecipe_WithExistingTag_ShouldReuseTag() {
        TagRequest tagRequest = new TagRequest();
        tagRequest.setName("Existing Tag");
        recipeRequest.setNewTags(List.of(tagRequest));

        Tag existingTag = Tag.builder()
                .tagId(UUID.randomUUID())
                .name("Existing Tag")
                .slug("existing-tag")
                .build();

        when(tagRepository.existsByNameIgnoreCase("Existing Tag")).thenReturn(true);
        when(tagRepository.findByNameIgnoreCase("Existing Tag")).thenReturn(Optional.of(existingTag));
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(tagRepository, never()).save(any(Tag.class));
        verify(recipeTagRepository).insertRecipeTag(eq(recipeId), eq(existingTag.getTagId()));
    }

    @Test
    void createRecipe_WithBothNewAndExistingCategories_ShouldMergeBoth() {
        UUID existingCategoryId = UUID.randomUUID();
        recipeRequest.setCategoryIds(List.of(existingCategoryId));

        CategoryRequest newCategoryReq = new CategoryRequest();
        newCategoryReq.setName("New Category");
        recipeRequest.setNewCategories(List.of(newCategoryReq));

        Category newCategory = Category.builder()
                .categoryId(UUID.randomUUID())
                .name("New Category")
                .build();

        when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());
        when(categoryMapper.toEntity(newCategoryReq)).thenReturn(newCategory);
        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(recipeCategoryRepository).insertRecipeCategory(eq(recipeId), eq(existingCategoryId));
        verify(recipeCategoryRepository).insertRecipeCategory(eq(recipeId), eq(newCategory.getCategoryId()));
    }

    @Test
    void createRecipe_WithBothNewAndExistingTags_ShouldMergeBoth() {
        UUID existingTagId = UUID.randomUUID();
        recipeRequest.setTagIds(List.of(existingTagId));

        TagRequest newTagReq = new TagRequest();
        newTagReq.setName("New Tag");
        recipeRequest.setNewTags(List.of(newTagReq));

        Tag newTag = Tag.builder()
                .tagId(UUID.randomUUID())
                .name("New Tag")
                .build();

        when(tagRepository.existsByNameIgnoreCase("New Tag")).thenReturn(false);
        when(tagMapper.toEntity(newTagReq)).thenReturn(newTag);
        when(tagRepository.save(any(Tag.class))).thenReturn(newTag);
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(recipeTagRepository).insertRecipeTag(eq(recipeId), eq(existingTagId));
        verify(recipeTagRepository).insertRecipeTag(eq(recipeId), eq(newTag.getTagId()));
    }

    @Test
    void createRecipe_WithBothNewAndExistingIngredients_ShouldMergeBoth() {
        UUID existingIngredientId = UUID.randomUUID();
        recipeRequest.setIngredients(List.of(existingIngredientId));

        IngredientRequest newIngReq = new IngredientRequest();
        newIngReq.setName("New Ingredient");
        recipeRequest.setNewIngredients(List.of(newIngReq));

        Ingredient newIngredient = Ingredient.builder()
                .ingredientId(UUID.randomUUID())
                .name("New Ingredient")
                .build();

        when(ingredientRepository.findByNameIgnoreCase("New Ingredient")).thenReturn(Optional.empty());
        when(ingredientMapper.toEntity(newIngReq)).thenReturn(newIngredient);
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(newIngredient);
        when(recipeMapper.toEntity(recipeRequest)).thenReturn(testRecipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLoaderHelper.loadRecipeDetailsForPublic(recipeId, userId)).thenReturn(recipeDetails);
        when(recipeMapper.toResponse(testRecipe)).thenReturn(recipeResponse);
        when(fileStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/recipes/test.jpg");

        RecipeResponse result = recipeService.createRecipe(recipeRequest);

        assertNotNull(result);
        verify(recipeIngredientRepository).insertRecipeIngredient(
                eq(recipeId), eq(existingIngredientId), isNull(), isNull(), isNull(), isNull());
        verify(recipeIngredientRepository).insertRecipeIngredient(
                eq(recipeId), eq(newIngredient.getIngredientId()), isNull(), isNull(), isNull(), isNull());
    }
}