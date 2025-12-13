package com.backend.cookshare.interaction.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.interaction.dto.response.RecipeLikeResponse;
import com.backend.cookshare.interaction.entity.RecipeLike;
import com.backend.cookshare.interaction.mapper.RecipeLikeMapper;
import com.backend.cookshare.interaction.repository.RecipeLikeRepository;
import com.backend.cookshare.interaction.sevice.impl.RecipeLikeServiceImpl;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.user.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeLikeServiceImplTest {

    @Mock
    private RecipeLikeRepository recipeLikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeLikeMapper recipeLikeMapper;

    @Mock
    private FirebaseStorageService firebaseStorageService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RecipeLikeServiceImpl recipeLikeService;

    private UUID recipeId;
    private UUID userId;
    private User user;
    private Recipe recipe;
    private RecipeLike recipeLike;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        recipeId = UUID.randomUUID();
        userId = UUID.randomUUID();

        user = User.builder()
                .userId(userId)
                .username("testuser")
                .build();

        recipe = Recipe.builder()
                .recipeId(recipeId)
                .likeCount(0)
                .build();

        recipeLike = RecipeLike.builder()
                .userId(userId)
                .recipeId(recipeId)
                .createdAt(LocalDateTime.now())
                .build();

        mockSecurityContext();
    }

    void mockSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testuser");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(context);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    }

    // ---------------------------------------------------------
    // ✔ TEST: likerecipe()
    // ---------------------------------------------------------
    @Test
    void testLikeRecipe_Success() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeLikeRepository.existsByUserIdAndRecipeId(userId, recipeId)).thenReturn(false);
        when(recipeLikeRepository.save(any())).thenReturn(recipeLike);

        RecipeLikeResponse mockResponse = RecipeLikeResponse.builder()
                .userId(userId)
                .recipeId(recipeId)
                .build();

        when(recipeLikeMapper.toRecipeLikeResponse(any())).thenReturn(mockResponse);
        when(recipeRepository.findUserIdByRecipeId(recipeId)).thenReturn(UUID.randomUUID());

        RecipeLikeResponse response = recipeLikeService.likerecipe(recipeId);

        assertNotNull(response);
        assertEquals(recipeId, response.getRecipeId());
        verify(notificationService).createLikeNotification(any(), eq(userId), eq(recipeId));
    }

    @Test
    void testLikeRecipe_AlreadyLiked_ShouldThrow() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeLikeRepository.existsByUserIdAndRecipeId(userId, recipeId)).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class,
                () -> recipeLikeService.likerecipe(recipeId));

        assertEquals(ErrorCode.RECIPE_ALREADY_LIKED, ex.getErrorCode());
    }

    // ---------------------------------------------------------
    // ✔ TEST: unlikerecipe()
    // ---------------------------------------------------------
    @Test
    void testUnlikeRecipe_Success() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeLikeRepository.findByUserIdAndRecipeId(userId, recipeId))
                .thenReturn(Optional.of(recipeLike));

        recipeLikeService.unlikerecipe(recipeId);

        verify(recipeLikeRepository).delete(recipeLike);
        verify(recipeRepository).save(any());
    }

    @Test
    void testUnlikeRecipe_NotFound_ShouldThrow() {
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeLikeRepository.findByUserIdAndRecipeId(userId, recipeId)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> recipeLikeService.unlikerecipe(recipeId));
    }

    // ---------------------------------------------------------
    // ✔ TEST: getallRecipeLiked()
    // ---------------------------------------------------------
    @Test
    void testGetAllRecipeLiked_Success() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<RecipeLike> page = new PageImpl<>(List.of(recipeLike), pageable, 1);

        when(recipeLikeRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(page);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeLikeMapper.toRecipeSummary(recipe)).thenReturn(
                com.backend.cookshare.interaction.dto.response.RecipeSummaryResponse.builder()
                        .recipeId(recipeId)
                        .featuredImage("img.png")
                        .build()
        );

        when(firebaseStorageService.convertPathToFirebaseUrl("img.png"))
                .thenReturn("firebase_url");

        when(recipeLikeMapper.toRecipeResponse(any(), any()))
                .thenReturn(RecipeLikeResponse.builder().recipeId(recipeId).build());

        PageResponse<RecipeLikeResponse> result = recipeLikeService.getallRecipeLiked(0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    // ---------------------------------------------------------
    // ✔ TEST: isRecipeLiked()
    // ---------------------------------------------------------
    @Test
    void testIsRecipeLiked_ReturnsTrue() {
        when(recipeLikeRepository.existsByUserIdAndRecipeId(userId, recipeId)).thenReturn(true);

        assertTrue(recipeLikeService.isRecipeLiked(recipeId));
    }

    // ---------------------------------------------------------
    // ✔ TEST: checkMultipleLikes()
    // ---------------------------------------------------------
    @Test
    void testCheckMultipleLikes() {
        List<UUID> ids = List.of(recipeId, UUID.randomUUID());

        when(recipeLikeRepository.findAllByUserIdAndRecipeIdIn(userId, ids))
                .thenReturn(List.of(recipeLike));

        Map<UUID, Boolean> result = recipeLikeService.checkMultipleLikes(ids);

        assertTrue(result.get(recipeId));
        assertFalse(result.get(ids.get(1)));
    }
}
