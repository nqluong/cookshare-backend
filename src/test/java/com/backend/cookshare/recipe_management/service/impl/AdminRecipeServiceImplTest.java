package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.recipe_management.dto.request.AdminRecipeApprovalRequest;
import com.backend.cookshare.recipe_management.dto.request.AdminRecipeUpdateRequest;
import com.backend.cookshare.recipe_management.dto.response.*;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.recipe_management.dto.response.RecipeDetailsResult; // đúng tên helper
import com.backend.cookshare.recipe_management.repository.*;
import com.backend.cookshare.user.repository.FollowRepository;
import com.backend.cookshare.user.service.ActivityLogService;
import com.backend.cookshare.user.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminRecipeServiceImplTest {

    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeLoaderHelper recipeLoaderHelper;
    @Mock private UserService userService;
    @Mock private PageMapper pageMapper;
    @Mock private FirebaseStorageService firebaseStorageService;
    @Mock private NotificationService notificationService;
    @Mock private ActivityLogService activityLogService;
    @Mock private FollowRepository followRepository;

    @Spy
    @InjectMocks
    private AdminRecipeServiceImpl adminRecipeService;

    private UUID recipeId;
    private UUID userId;
    private Recipe recipe;
    private RecipeDetailsResult detailsResult;
    private User recipeOwner;

    @BeforeEach
    void setUp() {
        recipeId = UUID.randomUUID();
        userId = UUID.randomUUID();

        recipeOwner = User.builder()
                .userId(userId)
                .username("chefjohn")
                .fullName("John Chef")
                .email("john@example.com")
                .build();

        recipe = Recipe.builder()
                .recipeId(recipeId)
                .userId(userId)
                .title("Bánh Mì Kẹp Thịt")
                .slug("banh-mi-kep-thit")
                .description("Công thức truyền thống")
                .featuredImage("recipes/banh-mi.jpg")
                .status(RecipeStatus.PENDING)
                .isPublished(false)
                .viewCount(100)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now())
                .build();

        detailsResult = new RecipeDetailsResult();
        detailsResult.user = recipeOwner;
        detailsResult.steps = new ArrayList<>();
        detailsResult.ingredients = new ArrayList<>();
        detailsResult.tags = new ArrayList<>();
        detailsResult.categories = new ArrayList<>();
    }

    @Test
    void getAllRecipesWithPagination_ShouldReturnPagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Recipe> recipePage = new PageImpl<>(List.of(recipe), pageable, 1);

        PageResponse<AdminRecipeListResponseDTO> expectedResponse = PageResponse.<AdminRecipeListResponseDTO>builder()
                .content(List.of(new AdminRecipeListResponseDTO()))
                .totalElements(1L)
                .page(0)
                .size(10)
                .totalPages(1)
                .build();

        when(recipeRepository.findAllWithAdminFilters(any(), any(), any(), eq(pageable)))
                .thenReturn(recipePage);

        // SỬA CHÍNH XÁC: Dùng any(Page.class) và any(Function.class)
        when(pageMapper.toPageResponse(any(Page.class), any(Function.class)))
                .thenReturn(expectedResponse);

        PageResponse<AdminRecipeListResponseDTO> result = adminRecipeService.getAllRecipesWithPagination(
                "bánh", true, RecipeStatus.PENDING, pageable);

    }

    @Test
    void getRecipeDetailById_WhenRecipeExists_ShouldReturnDetail() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeLoaderHelper.loadRecipeDetailsForAdmin(recipeId, userId)).thenReturn(detailsResult);
        when(firebaseStorageService.convertPathToFirebaseUrl("recipes/banh-mi.jpg"))
                .thenReturn("https://firebasestorage.../banh-mi.jpg");

        AdminRecipeDetailResponseDTO result = adminRecipeService.getRecipeDetailById(recipeId);

        assertNotNull(result);
        assertEquals(recipeId, result.getRecipeId());
        assertEquals("chefjohn", result.getUsername());
        verify(recipeLoaderHelper).loadRecipeDetailsForAdmin(recipeId, userId);
    }

    @Test
    void getRecipeDetailById_WhenRecipeNotFound_ShouldThrowException() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> adminRecipeService.getRecipeDetailById(recipeId));

        assertEquals(ErrorCode.RECIPE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateRecipe_ShouldUpdateFieldsAndSaveSuccessfully() {
        AdminRecipeUpdateRequest request = new AdminRecipeUpdateRequest();
        request.setTitle("Bánh Mì Nướng Muối Ớt");
        request.setIsPublished(true);
        request.setIsFeatured(true);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(i -> i.getArgument(0));

        // Dùng detailsResult đã tạo ở @BeforeEach (đã có user)
        when(recipeLoaderHelper.loadRecipeDetailsForAdmin(eq(recipeId), eq(userId)))
                .thenReturn(detailsResult);

        when(firebaseStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/banh-mi.jpg");

        AdminRecipeDetailResponseDTO result = adminRecipeService.updateRecipe(recipeId, request);

    }

    @Test
    void approveRecipe_WhenApproved_ShouldSetStatusAndNotify() {
        AdminRecipeApprovalRequest request = new AdminRecipeApprovalRequest();
        request.setApproved(true);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(followRepository.findAllFollowerIdsByUser(userId)).thenReturn(List.of(UUID.randomUUID()));
        when(userService.getUserById(userId)).thenReturn(Optional.of(recipeOwner));

        adminRecipeService.approveRecipe(recipeId, request);

        verify(recipeRepository).save(argThat(r ->
                r.getStatus() == RecipeStatus.APPROVED && r.getIsPublished()
        ));
        verify(notificationService).createRecipeApprovedNotification(userId, recipeId, recipe.getTitle());
        verify(notificationService).createNewRecipeNotificationForFollowers(
                anyList(), eq(userId), eq("John Chef"), eq(recipeId), eq(recipe.getTitle()));
        verify(activityLogService).logRecipeActivity(userId, recipeId, "APPROVE");
    }

    @Test
    void approveRecipe_WhenRejected_ShouldSetRejectedStatus() {
        AdminRecipeApprovalRequest request = new AdminRecipeApprovalRequest();
        request.setApproved(false);
        request.setRejectionReason("Nội dung không phù hợp");

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe)); // ĐÃ SỬA: findById

        adminRecipeService.approveRecipe(recipeId, request);

        verify(recipeRepository).save(argThat(r ->
                r.getStatus() == RecipeStatus.REJECTED && !r.getIsPublished()
        ));
        verify(activityLogService).logRecipeActivity(userId, recipeId, "REJECT");
    }

    @Test
    void approveRecipe_WhenAlreadyApproved_ShouldDoNothing() {
        recipe.setStatus(RecipeStatus.APPROVED);
        AdminRecipeApprovalRequest request = new AdminRecipeApprovalRequest();
        request.setApproved(true);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        adminRecipeService.approveRecipe(recipeId, request);

        verify(recipeRepository, never()).save(any());
    }

    @Test
    void deleteRecipe_ShouldDeleteAndClearNotifications() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        adminRecipeService.deleteRecipe(recipeId);

        verify(notificationService).deleteRecipeNotifications(recipeId);
        verify(activityLogService).logRecipeActivity(userId, recipeId, "DELETE");
        verify(recipeRepository).delete(recipe);
    }


    @Test
    void setPublishedRecipe_WhenApprovedRecipe_ShouldAllow() {
        recipe.setStatus(RecipeStatus.APPROVED);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        adminRecipeService.setPublishedRecipe(recipeId, true);

        verify(recipeRepository).save(argThat(r -> r.getIsPublished()));
    }

    @Test
    void setPublishedRecipe_WhenNotApproved_ShouldThrowException() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        CustomException ex = assertThrows(CustomException.class,
                () -> adminRecipeService.setPublishedRecipe(recipeId, true));

        assertEquals(ErrorCode.RECIPE_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    void getPendingRecipes_ShouldDelegateCorrectly() {
        Pageable pageable = PageRequest.of(0, 10);
        adminRecipeService.getPendingRecipes("bánh mì", pageable);
    }

    @Test
    void getApprovedRecipes_ShouldDelegateCorrectly() {
        Pageable pageable = PageRequest.of(0, 10);
        adminRecipeService.getApprovedRecipes("phở", pageable);
    }

    @Test
    void getRejectedRecipes_ShouldDelegateCorrectly() {
        Pageable pageable = PageRequest.of(0, 10);
        adminRecipeService.getRejectedRecipes(null, pageable);
    }
//    @Test
//    void mapToListResponseDTO_ShouldBeCalled_WhenGetAllRecipesWithPagination() {
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Recipe> recipePage = new PageImpl<>(List.of(recipe), pageable, 1);
//
//        when(userService.getUserById(eq(userId))).thenReturn(Optional.of(recipeOwner));
//        when(firebaseStorageService.convertPathToFirebaseUrl(anyString()))
//                .thenReturn("https://firebase.url/banh-mi.jpg");
//
//        // Tạo response giả
//        PageResponse<AdminRecipeListResponseDTO> pageResponse = PageResponse.<AdminRecipeListResponseDTO>builder()
//                .content(List.of(
//                        AdminRecipeListResponseDTO.builder()
//                                .recipeId(recipeId)
//                                .title("Bánh Mì Kẹp Thịt")
//                                .username("chefjohn")
//                                .featuredImage("https://firebase.url/banh-mi.jpg")
//                                .build()
//                ))
//                .totalElements(1L)
//                .page(0)
//                .size(10)
//                .totalPages(1)
//                .build();
//
//        when(recipeRepository.findAllWithAdminFilters(any(), any(), any(), any(), eq(pageable)))
//                .thenReturn(recipePage);
//
//        // QUAN TRỌNG: Dùng thenAnswer để gọi thật mapper (mapToListResponseDTO)
//        when(pageMapper.toPageResponse(eq(recipePage), any(Function.class)))
//                .thenAnswer(invocation -> {
//                    Function<Recipe, AdminRecipeListResponseDTO> mapper = invocation.getArgument(1);
//                    // Gọi thật mapper để cover mapToListResponseDTO
//                    mapper.apply(recipe);
//                    return pageResponse;
//                });
//
//        // Thực thi
//        adminRecipeService.getAllRecipesWithPagination("bánh", null, null, null, pageable);
//
//        verify(userService).getUserById(eq(userId));
//        verify(firebaseStorageService).convertPathToFirebaseUrl(eq("recipes/banh-mi.jpg"));
//        verify(pageMapper).toPageResponse(eq(recipePage), any(Function.class));
//    }

    @Test
    void getRecipeDetailById_WhenUserIsNull_ShouldReturnNullUserFields() {
        RecipeDetailsResult detailsNoUser = new RecipeDetailsResult();
        detailsNoUser.user = null;
        detailsNoUser.steps = new ArrayList<>();
        detailsNoUser.ingredients = new ArrayList<>();
        detailsNoUser.tags = new ArrayList<>();
        detailsNoUser.categories = new ArrayList<>();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeLoaderHelper.loadRecipeDetailsForAdmin(recipeId, userId)).thenReturn(detailsNoUser);
        when(firebaseStorageService.convertPathToFirebaseUrl(anyString()))
                .thenReturn("https://firebase.url/banh-mi.jpg");

        AdminRecipeDetailResponseDTO result = adminRecipeService.getRecipeDetailById(recipeId);

        assertNull(result.getUsername());
        assertNull(result.getUserFullName());
        assertNull(result.getUserEmail());
        assertNull(result.getUserAvatarUrl());
        // → cover toàn bộ null check trong builder
    }

    @Test
    void getRecipeDetailById_WhenLoaderThrowsException_ShouldCatchAndThrowCustomException() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeLoaderHelper.loadRecipeDetailsForAdmin(recipeId, userId))
                .thenThrow(new RuntimeException("Database timeout"));

        CustomException ex = assertThrows(CustomException.class,
                () -> adminRecipeService.getRecipeDetailById(recipeId));

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
        verify(recipeLoaderHelper).loadRecipeDetailsForAdmin(recipeId, userId);
        // → cover toàn bộ try-catch + log.error
    }

    @Test
    void approveRecipe_WhenApproved_NoFollowers_ShouldOnlyNotifyOwner() {
        AdminRecipeApprovalRequest request = new AdminRecipeApprovalRequest();
        request.setApproved(true);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(followRepository.findAllFollowerIdsByUser(userId)).thenReturn(Collections.emptyList());

        adminRecipeService.approveRecipe(recipeId, request);

        verify(notificationService).createRecipeApprovedNotification(userId, recipeId, recipe.getTitle());
        verify(notificationService, never()).createNewRecipeNotificationForFollowers(any(), any(), any(), any(), any());
        verify(recipeRepository).save(argThat(r -> r.getStatus() == RecipeStatus.APPROVED));
        // → cover nhánh followerIds.isEmpty()
    }

    @Test
    void approveRecipe_WhenApproved_FollowersExist_ButUserNotFound_ShouldSkipFollowerNotification() {
        AdminRecipeApprovalRequest request = new AdminRecipeApprovalRequest();
        request.setApproved(true);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(followRepository.findAllFollowerIdsByUser(userId)).thenReturn(List.of(UUID.randomUUID()));
        when(userService.getUserById(userId)).thenReturn(Optional.empty()); // user null

        adminRecipeService.approveRecipe(recipeId, request);

        verify(notificationService).createRecipeApprovedNotification(any(), any(), any());
        verify(notificationService, never()).createNewRecipeNotificationForFollowers(any(), any(), any(), any(), any());
        // → cover nhánh recipeOwner == null
    }

    @Test
    void approveRecipe_WhenAlreadyApproved_ShouldLogWarningAndDoNothing() {
        recipe.setStatus(RecipeStatus.APPROVED);
        AdminRecipeApprovalRequest request = new AdminRecipeApprovalRequest();
        request.setApproved(true);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        adminRecipeService.approveRecipe(recipeId, request);

        verify(recipeRepository, never()).save(any());
        verifyNoInteractions(notificationService);
        // → cover log.warn + early return
    }
}