package com.backend.cookshare.authentication.service;

import com.backend.cookshare.authentication.dto.UserProfileDto;
import com.backend.cookshare.authentication.dto.request.AvatarUploadUrlRequest;
import com.backend.cookshare.authentication.dto.request.UpdateUserProfileRequest;
import com.backend.cookshare.authentication.dto.response.AvatarUploadUrlResponse;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileService {
    /**
     * Lấy thông tin profile của user theo ID
     */
    Optional<UserProfileDto> getUserProfileById(UUID userId);

    /**
     * Lấy thông tin profile của user theo username
     */
    Optional<UserProfileDto> getUserProfileByUsername(String username);

    /**
     * Cập nhật profile của user
     */
    UserProfileDto updateUserProfile(UUID userId, UpdateUserProfileRequest request);

    /**
     * Tạo URL để upload avatar
     */
    AvatarUploadUrlResponse generateAvatarUploadUrl(UUID userId, AvatarUploadUrlRequest request);
}

