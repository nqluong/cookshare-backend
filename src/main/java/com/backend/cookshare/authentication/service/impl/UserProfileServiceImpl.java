package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.UserProfileDto;
import com.backend.cookshare.authentication.dto.request.AvatarUploadUrlRequest;
import com.backend.cookshare.authentication.dto.request.UpdateUserProfileRequest;
import com.backend.cookshare.authentication.dto.response.AvatarUploadUrlResponse;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.UserProfileService;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final UserService userService;
    private final RecipeRepository recipeRepository;

    @Override
    public Optional<UserProfileDto> getUserProfileById(UUID userId) {
        log.info("Fetching user profile by ID: {}", userId);

        return userService.getUserById(userId)
                .map(this::buildUserProfileDto);
    }

    @Override
    public Optional<UserProfileDto> getUserProfileByUsername(String username) {
        log.info("Fetching user profile by username: {}", username);

        return userService.getUserByUsername(username)
                .map(this::buildUserProfileDto);
    }

    @Override
    public UserProfileDto updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        log.info("Updating user profile for userId: {}", userId);

        User updatedUser = userService.updateUserProfile(userId, request);

        return buildUserProfileDto(updatedUser);
    }

    @Override
    public AvatarUploadUrlResponse generateAvatarUploadUrl(UUID userId, AvatarUploadUrlRequest request) {
        log.info("Generating avatar upload URL for userId: {}", userId);

        return userService.generateAvatarUploadUrl(userId, request);
    }

    /**
     * Helper method để build UserProfileDto từ User entity
     */
    private UserProfileDto buildUserProfileDto(User user) {
        int totalLikes = recipeRepository.getTotalLikeCountByUserId(user.getUserId());

        return UserProfileDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .lastActive(user.getLastActive())
                .followerCount(user.getFollowerCount())
                .followingCount(user.getFollowingCount())
                .recipeCount(user.getRecipeCount())
                .totalLikes(totalLikes)
                .createdAt(user.getCreatedAt())
                .build();
    }
}

