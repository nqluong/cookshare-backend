package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.interaction.dto.response.RecipeSummaryResponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.mapper.RecipeMapper;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.user.dto.*;
import com.backend.cookshare.user.entity.Follow;
import com.backend.cookshare.user.repository.FollowRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    @Mock private NotificationService notificationService;
    @Mock private PageMapper pageMapper;
    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeMapper recipeMapper;
    @Mock private FirebaseStorageService firebaseStorageService;

    @InjectMocks private FollowService followService;

    private UUID currentUserId;
    private UUID targetUserId;
    private User currentUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        currentUser = User.builder()
                .userId(currentUserId)
                .username("current")
                .fullName("Current User")
                .avatarUrl("current.jpg")
                .isActive(true)
                .followerCount(10)
                .followingCount(5)
                .build();

        targetUser = User.builder()
                .userId(targetUserId)
                .username("target")
                .fullName("Target User")
                .avatarUrl("target.jpg")
                .isActive(true)
                .followerCount(20)
                .followingCount(15)
                .build();
    }

    private void mockCurrentUserInSecurityContext() {
        var auth = new UsernamePasswordAuthenticationToken("current", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByUsername("current")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== followUser() ====================

    @Test
    void followUser_Success() {
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(false);

        when(followRepository.save(any(Follow.class))).thenAnswer(i -> {
            Follow f = i.getArgument(0);
            f.setCreatedAt(LocalDateTime.now());
            return f;
        });

        FollowResponse response = followService.followUser(currentUserId, targetUserId);

        verify(followRepository).save(any(Follow.class));
        verify(userRepository, times(2)).save(any(User.class));
        verify(notificationService).createFollowNotification(targetUserId, currentUserId);

        assertEquals(21, targetUser.getFollowerCount());
        assertEquals(6, currentUser.getFollowingCount());
        assertEquals("Đã follow thành công", response.getMessage());
    }

    @Test
    void followUser_FollowerNotActive() {
        currentUser.setIsActive(false);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        // Không mock targetUserId → vì không được gọi

        assertThrows(CustomException.class, () -> followService.followUser(currentUserId, targetUserId));
    }

    @Test
    void followUser_FollowingNotActive() {
        targetUser.setIsActive(false);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThrows(CustomException.class, () -> followService.followUser(currentUserId, targetUserId));
    }

    @Test
    void followUser_FollowingNotFound() {
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> followService.followUser(currentUserId, targetUserId));
    }

    @Test
    void followUser_FollowerNotFound() {
        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());
        // Không mock targetUserId

        assertThrows(CustomException.class, () -> followService.followUser(currentUserId, targetUserId));
    }

    @Test
    void followUser_AlreadyFollowing() {
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(true);

        assertThrows(CustomException.class, () -> followService.followUser(currentUserId, targetUserId));
    }
    // ==================== unfollowUser() ====================

    @Test
    void unfollowUser_Success() {
        Follow follow = Follow.builder().followerId(currentUserId).followingId(targetUserId).build();
        when(followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(Optional.of(follow));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        followService.unfollowUser(currentUserId, targetUserId);

        verify(followRepository).delete(follow);
        verify(notificationService).deleteFollowNotification(targetUserId, currentUserId);
        verify(userRepository, times(2)).save(any(User.class));
        assertEquals(19, targetUser.getFollowerCount());
        assertEquals(4, currentUser.getFollowingCount());
    }

    @Test void unfollowUser_NotFollowing() { when(followRepository.findByFollowerIdAndFollowingId(any(), any())).thenReturn(Optional.empty()); assertThrows(CustomException.class, () -> followService.unfollowUser(currentUserId, targetUserId)); }

    // ==================== getFollowers() ====================

    @Test
    void getFollowers_ShouldReturnPagedDto() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UUID> idPage = new PageImpl<>(List.of(currentUserId), pageable, 1);

        when(followRepository.findFollowerIds(targetUserId, pageable)).thenReturn(idPage);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(followRepository.existsByFollowerIdAndFollowingId(targetUserId, currentUserId)).thenReturn(true);

        // Fix lỗi generic
        when(pageMapper.toPageResponse(anyList(), eq(idPage)))
                .thenAnswer(invocation -> PageResponse.<UserFollowDto>builder()
                        .content(invocation.getArgument(0))
                        .page(0).size(10).totalElements(1L).totalPages(1).build());

        PageResponse<UserFollowDto> result = followService.getFollowers(targetUserId, 0, 10);
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getIsFollowing());
    }

    @Test
    void getFollowers_ShouldSkipNullUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UUID> idPage = new PageImpl<>(List.of(UUID.randomUUID()), pageable, 1);

        when(followRepository.findFollowerIds(targetUserId, pageable)).thenReturn(idPage);
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        when(pageMapper.toPageResponse(anyList(), eq(idPage)))
                .thenAnswer(i -> PageResponse.<UserFollowDto>builder().content(i.getArgument(0)).build());

        PageResponse<UserFollowDto> result = followService.getFollowers(targetUserId, 0, 10);
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== getFollowing() ====================

    @Test
    void getFollowing_ShouldReturnPagedDto() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UUID> idPage = new PageImpl<>(List.of(targetUserId), pageable, 1);

        when(followRepository.findFollowingIds(currentUserId, pageable)).thenReturn(idPage);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)).thenReturn(true);

        when(pageMapper.toPageResponse(anyList(), eq(idPage)))
                .thenAnswer(i -> PageResponse.<UserFollowDto>builder().content(i.getArgument(0)).build());

        PageResponse<UserFollowDto> result = followService.getFollowing(currentUserId, 0, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getFollowing_ShouldSkipNullUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UUID> idPage = new PageImpl<>(List.of(UUID.randomUUID()), pageable, 1);

        when(followRepository.findFollowingIds(currentUserId, pageable)).thenReturn(idPage);
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        when(pageMapper.toPageResponse(anyList(), eq(idPage)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<UserFollowDto> content = invocation.getArgument(0);
                    return PageResponse.<UserFollowDto>builder()
                            .content(content != null ? content : List.of())
                            .page(0)
                            .size(10)
                            .totalElements(content != null ? (long) content.size() : 0L)
                            .totalPages(1)
                            .build();
                });

        PageResponse<UserFollowDto> result = followService.getFollowing(currentUserId, 0, 10);
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== isFollowing() ====================

    @Test void isFollowing_ReturnsTrue() { when(followRepository.existsByFollowerIdAndFollowingId(any(), any())).thenReturn(true); assertTrue(followService.isFollowing(currentUserId, targetUserId)); }
    @Test void isFollowing_ReturnsFalse() { when(followRepository.existsByFollowerIdAndFollowingId(any(), any())).thenReturn(false); assertFalse(followService.isFollowing(currentUserId, targetUserId)); }

    // ==================== getRecipesByFollowing() ====================

    @Test
    void getRecipesByFollowing_ShouldReturnRecipes() {
        mockCurrentUserInSecurityContext();
        List<UUID> followingIds = List.of(targetUserId);
        Recipe recipe = Recipe.builder().recipeId(UUID.randomUUID()).userId(targetUserId).title("Delicious").featuredImage("img.jpg").build();
        Page<Recipe> page = new PageImpl<>(List.of(recipe));

        when(followRepository.findAllFollowingIdsByUser(currentUserId)).thenReturn(followingIds);
        when(recipeRepository.findRecipesByFollowingIds(eq(followingIds), any())).thenReturn(page);
        when(recipeMapper.toRecipeSummary(recipe)).thenReturn(RecipeSummaryResponse.builder().featuredImage("img.jpg").build());
        when(firebaseStorageService.convertPathToFirebaseUrl("img.jpg")).thenReturn("https://url");
        when(recipeMapper.toRecipeByFollowingResponse(eq(recipe), any())).thenReturn(RecipeByFollowingResponse.builder().recipe(RecipeSummaryResponse.builder().build()).build());

        PageResponse<RecipeByFollowingResponse> result = followService.getRecipesByFollowing(0, 10);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getRecipesByFollowing_ShouldHandleNullSummary() {
        mockCurrentUserInSecurityContext();
        Recipe recipe = Recipe.builder().recipeId(UUID.randomUUID()).userId(targetUserId).build();
        Page<Recipe> page = new PageImpl<>(List.of(recipe));

        when(followRepository.findAllFollowingIdsByUser(currentUserId)).thenReturn(List.of(targetUserId));
        when(recipeRepository.findRecipesByFollowingIds(any(), any())).thenReturn(page);
        when(recipeMapper.toRecipeSummary(recipe)).thenReturn(null);

        when(recipeMapper.toRecipeByFollowingResponse(eq(recipe), isNull())).thenReturn(RecipeByFollowingResponse.builder().build());

        PageResponse<RecipeByFollowingResponse> result = followService.getRecipesByFollowing(0, 10);
        assertEquals(1, result.getContent().size());
        verify(firebaseStorageService, never()).convertPathToFirebaseUrl(anyString());
    }

    @Test
    void getRecipesByFollowing_NoFollowing_ShouldReturnEmpty() {
        mockCurrentUserInSecurityContext();
        when(followRepository.findAllFollowingIdsByUser(currentUserId)).thenReturn(List.of());
        when(recipeRepository.findRecipesByFollowingIds(eq(List.of()), any())).thenReturn(Page.empty());

        PageResponse<RecipeByFollowingResponse> result = followService.getRecipesByFollowing(0, 10);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getRecipesByFollowing_ShouldThrow_WhenUserNotFound() {
        var auth = new UsernamePasswordAuthenticationToken("unknown", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> followService.getRecipesByFollowing(0, 10));
    }
}