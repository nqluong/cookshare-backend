package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.mapper.RecipeMapper;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.user.dto.*;
import com.backend.cookshare.user.entity.Follow;
import com.backend.cookshare.user.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final NotificationService notificationService;
    private final PageMapper pageMapper;
    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;
    private final FirebaseStorageService firebaseStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public FollowResponse followUser(UUID followerId, UUID followingId) {
        log.info("User {} attempting to follow user {}", followerId, followingId);

        // Kiểm tra không thể follow chính mình
        if (followerId.equals(followingId)) {
            throw new CustomException(ErrorCode.CANNOT_FOLLOW_YOURSELF);
        }

        // Kiểm tra người follow có tồn tại và active
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!follower.getIsActive()) {
            throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
        }

        // Kiểm tra người được follow có tồn tại và active
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!following.getIsActive()) {
            throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
        }

        // Kiểm tra đã follow chưa
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new CustomException(ErrorCode.ALREADY_FOLLOWING);
        }

        // Tạo quan hệ follow
        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .build();

        followRepository.save(follow);

        // Cập nhật follower count
        following.setFollowerCount(following.getFollowerCount() + 1);
        follower.setFollowingCount(follower.getFollowingCount() + 1);

        userRepository.save(following);
        userRepository.save(follower);

        notificationService.createFollowNotification(followingId, followerId);

        sendFollowWebSocketMessage("FOLLOW", follower, following);

        log.info("User {} successfully followed user {}", followerId, followingId);

        return FollowResponse.builder()
                .followerId(followerId)
                .followingId(followingId)
                .followerUsername(follower.getUsername())
                .followingUsername(following.getUsername())
                .createdAt(follow.getCreatedAt())
                .message("Đã follow thành công")
                .build();
    }

    @Transactional
    public void unfollowUser(UUID followerId, UUID followingId) {
        log.info("User {} attempting to unfollow user {}", followerId, followingId);

        // Kiểm tra quan hệ follow tồn tại
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOLLOWING));

        notificationService.deleteFollowNotification(followingId, followerId);

        // Xóa quan hệ follow
        followRepository.delete(follow);

        // Cập nhật follower count
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        following.setFollowerCount(Math.max(0, following.getFollowerCount() - 1));
        follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));

        userRepository.save(following);
        userRepository.save(follower);

        sendFollowWebSocketMessage("UNFOLLOW", follower, following);

        log.info("User {} successfully unfollowed user {}", followerId, followingId);
    }

    private void sendFollowWebSocketMessage(
            String action,
            User follower,
            User following
    ) {
        try {
            FollowWebSocketMessage message = FollowWebSocketMessage.builder()
                    .action(action)
                    .followerId(follower.getUserId())
                    .followingId(following.getUserId())
                    .followerUsername(follower.getUsername())
                    .followerFullName(follower.getFullName())
                    .followerAvatarUrl(follower.getAvatarUrl())
                    .followingCount(follower.getFollowingCount())
                    .followerCount(following.getFollowerCount())
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    following.getUserId().toString(),
                    "/queue/follow",
                    message
            );

            log.info("WebSocket {} sent: follower={}, following={}",
                    action, follower.getUserId(), following.getUserId());

        } catch (Exception e) {
            log.error("Failed to send follow WS", e);
        }
    }


    @Transactional(readOnly = true)
    public PageResponse<UserFollowDto> getFollowers(UUID userId, int page, int size) {
        log.info("Getting followers for user {}", userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> followerIds = followRepository.findFollowerIds(userId, pageable);

        List<UserFollowDto> followers = followerIds.getContent().stream()
                .map(followerId -> {
                    User user = userRepository.findById(followerId).orElse(null);
                    if (user != null) {
                        return mapToUserFollowDto(user, userId);
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return pageMapper.toPageResponse(followers, followerIds);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserFollowDto> getFollowing(UUID userId, int page, int size) {
        log.info("Getting following for user {}", userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> followingIds = followRepository.findFollowingIds(userId, pageable);

        List<UserFollowDto> following = followingIds.getContent().stream()
                .map(followingId -> {
                    User user = userRepository.findById(followingId).orElse(null);
                    if (user != null) {
                        return mapToUserFollowDto(user, userId);
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return pageMapper.toPageResponse(following, followingIds);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    private UserFollowDto mapToUserFollowDto(User user, UUID currentUserId) {
        return UserFollowDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .followerCount(user.getFollowerCount())
                .followingCount(user.getFollowingCount())
                .isFollowing(currentUserId != null ?
                        followRepository.existsByFollowerIdAndFollowingId(currentUserId, user.getUserId()) : null)
                .build();
    }

    @Transactional
    public PageResponse<RecipeByFollowingResponse> getRecipesByFollowing(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        List<UUID> followingIds = followRepository.findAllFollowingIdsByUser(currentUser.getUserId());
        Page<Recipe> recipes = recipeRepository.findRecipesByFollowingIds(followingIds, pageable);
        Page<RecipeByFollowingResponse> responsePage = recipes.map(recipe -> {
            var summary = recipeMapper.toRecipeSummary(recipe);
            if (summary != null && summary.getFeaturedImage() != null) {
                summary.setFeaturedImage(firebaseStorageService.convertPathToFirebaseUrl(summary.getFeaturedImage()));
            }
            var response = recipeMapper.toRecipeByFollowingResponse(recipe, summary);
            response.setFollowerId(currentUser.getUserId());
            return response;
        });
        return PageResponse.<RecipeByFollowingResponse>builder()
                .page(page)
                .size(size)
                .totalPages(responsePage.getTotalPages())
                .totalElements(responsePage.getTotalElements())
                .content(responsePage.getContent())
                .build();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}