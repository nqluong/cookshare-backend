package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.response.AdminUserDetailResponseDTO;
import com.backend.cookshare.authentication.dto.response.AdminUserListResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.AdminUserService;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.mapper.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserService userService;
    private final PageMapper pageMapper;

    @Override
    public PageResponse<AdminUserListResponseDTO> getAllUsersWithPagination(String search, Pageable pageable) {
        log.info("Admin đang lấy danh sách người dùng với từ khóa tìm kiếm: {}, trang: {}, kích thước: {}", 
                search, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> userPage = userService.getAllUsersWithPagination(search, pageable);
        
        return pageMapper.toPageResponse(userPage, this::mapToListResponseDTO);
    }

    @Override
    public AdminUserDetailResponseDTO getUserDetailById(UUID userId) {
        log.info("Admin đang lấy thông tin chi tiết người dùng với userId: {}", userId);
        
        User user = userService.getUserDetailById(userId);
        return mapToDetailResponseDTO(user);
    }

    @Override
    public void banUser(UUID userId, String reason) {
        log.info("Admin đang cấm người dùng: {} với lý do: {}", userId, reason);
        userService.banUser(userId, reason);
    }

    @Override
    public void unbanUser(UUID userId) {
        log.info("Admin đang gỡ cấm người dùng: {}", userId);
        userService.unbanUser(userId);
    }

    @Override
    public void deleteUser(UUID userId) {
        log.info("Admin đang xóa người dùng: {}", userId);
        userService.deleteUserByAdmin(userId);
    }

    @Override
    public AdminUserDetailResponseDTO updateUser(UUID userId, com.backend.cookshare.authentication.dto.request.AdminUpdateUserRequest request) {
        log.info("Admin đang cập nhật thông tin người dùng: {}", userId);
        
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
        
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userService.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username đã tồn tại");
            }
            user.setUsername(request.getUsername());
        }
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userService.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email đã tồn tại");
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        
        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }
        
        User updatedUser = userService.updateUser(user);
        
        return mapToDetailResponseDTO(updatedUser);
    }


    // Chuyển đổi User entity thành AdminUserListResponseDTO
    private AdminUserListResponseDTO mapToListResponseDTO(User user) {
        return AdminUserListResponseDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .followerCount(user.getFollowerCount())
                .followingCount(user.getFollowingCount())
                .recipeCount(user.getRecipeCount())
                .lastActive(user.getLastActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // Chuyển đổi User entity thành AdminUserDetailResponseDTO
    private AdminUserDetailResponseDTO mapToDetailResponseDTO(User user) {
        return AdminUserDetailResponseDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .googleId(user.getGoogleId())
                .facebookId(user.getFacebookId())
                .followerCount(user.getFollowerCount())
                .followingCount(user.getFollowingCount())
                .recipeCount(user.getRecipeCount())
                .lastActive(user.getLastActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
