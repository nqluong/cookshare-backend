package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.UserProfileDto;
import com.backend.cookshare.authentication.dto.request.AvatarUploadUrlRequest;
import com.backend.cookshare.authentication.dto.request.UpdateUserProfileRequest;
import com.backend.cookshare.authentication.dto.response.AvatarUploadUrlResponse;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();

        user = User.builder()
                .userId(userId)
                .username("tester")
                .email("tester@example.com")
                .fullName("Test User")
                .avatarUrl("avatar.png")
                .bio("Hello world")
                .role(null)
                .isActive(true)
                .emailVerified(true)
                .lastActive(LocalDateTime.now())
                .followerCount(10)
                .followingCount(5)
                .recipeCount(3)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetUserProfileById() {
        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(recipeRepository.getTotalLikeCountByUserId(userId)).thenReturn(15);

        Optional<UserProfileDto> result = userProfileService.getUserProfileById(userId);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
        assertEquals(15, result.get().getTotalLikes());
        verify(userService).getUserById(userId);
        verify(recipeRepository).getTotalLikeCountByUserId(userId);
    }

    @Test
    void testGetUserProfileByUsername() {
        String username = "tester";
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));
        when(recipeRepository.getTotalLikeCountByUserId(userId)).thenReturn(15);

        Optional<UserProfileDto> result = userProfileService.getUserProfileByUsername(username);

        assertTrue(result.isPresent());
        assertEquals("tester", result.get().getUsername());
        assertEquals(15, result.get().getTotalLikes());
        verify(userService).getUserByUsername(username);
        verify(recipeRepository).getTotalLikeCountByUserId(userId);
    }

    @Test
    void testUpdateUserProfile() {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .fullName("Updated User")
                .bio("Updated bio")
                .build();

        User updatedUser = User.builder()
                .userId(userId)
                .username("tester")
                .fullName("Updated User")
                .bio("Updated bio")
                .avatarUrl("avatar.png")
                .role(null)
                .isActive(true)
                .emailVerified(true)
                .lastActive(LocalDateTime.now())
                .followerCount(10)
                .followingCount(5)
                .recipeCount(3)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.updateUserProfile(userId, request)).thenReturn(updatedUser);
        when(recipeRepository.getTotalLikeCountByUserId(userId)).thenReturn(20);

        UserProfileDto result = userProfileService.updateUserProfile(userId, request);

        assertNotNull(result);
        assertEquals("Updated User", result.getFullName());
        assertEquals("Updated bio", result.getBio());
        assertEquals(20, result.getTotalLikes());
        verify(userService).updateUserProfile(userId, request);
        verify(recipeRepository).getTotalLikeCountByUserId(userId);
    }

    @Test
    void testGenerateAvatarUploadUrl() {
        AvatarUploadUrlRequest request = new AvatarUploadUrlRequest("avatar.png", "image/png");
        AvatarUploadUrlResponse responseMock = new AvatarUploadUrlResponse("signedUrl", "publicUrl");

        when(userService.generateAvatarUploadUrl(userId, request)).thenReturn(responseMock);

        AvatarUploadUrlResponse response = userProfileService.generateAvatarUploadUrl(userId, request);

        assertNotNull(response);
        assertEquals("signedUrl", response.getUploadUrl());
        assertEquals("publicUrl", response.getPublicUrl());
        verify(userService).generateAvatarUploadUrl(userId, request);
    }
}
