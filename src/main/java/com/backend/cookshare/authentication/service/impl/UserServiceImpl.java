package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.request.UpdateUserProfileRequest;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String createUser(UserRequest user) {

        User saveUser = User.builder()
                .username(user.getUsername())
                .passwordHash(passwordEncoder.encode(user.getPassword()))
                .email(user.getEmail())
                .fullName(user.getFullname())
                .createdAt(LocalDateTime.now())
                .lastActive(LocalDateTime.now())
                .build();
        userRepository.save(saveUser);
        return "Ban da dang ky thanh cong!";
    }

    @Override
    public Optional<User> getUserById(UUID userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> getUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(UUID userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void updateLastActive(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActive(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Override
    public void updateUserToken(String token, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRefreshToken(token);
        this.userRepository.save(user);
    }

    @Override
    public User getUserByRefreshTokenAndUsername(String token, String username) {
        return this.userRepository.findByRefreshTokenAndUsername(token, username);
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        // Tìm user theo username
        User user = getUserByUsernameOrEmail(username)
                .orElseThrow(() -> new com.backend.cookshare.common.exception.CustomException(
                        com.backend.cookshare.common.exception.ErrorCode.USER_NOT_FOUND));

        // Kiểm tra mật khẩu hiện tại có đúng không
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new com.backend.cookshare.common.exception.CustomException(
                    com.backend.cookshare.common.exception.ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // Kiểm tra mật khẩu mới có trùng với mật khẩu hiện tại không
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new com.backend.cookshare.common.exception.CustomException(
                    com.backend.cookshare.common.exception.ErrorCode.SAME_PASSWORD);
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public User updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        // Tìm user theo ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.backend.cookshare.common.exception.CustomException(
                        com.backend.cookshare.common.exception.ErrorCode.USER_NOT_FOUND));

        // Kiểm tra nếu username mới đã tồn tại (và không phải của chính user này)
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new com.backend.cookshare.common.exception.CustomException(
                        com.backend.cookshare.common.exception.ErrorCode.USERNAME_EXISTED);
            }
            user.setUsername(request.getUsername());
        }

        // Kiểm tra nếu email mới đã tồn tại (và không phải của chính user này)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new com.backend.cookshare.common.exception.CustomException(
                        com.backend.cookshare.common.exception.ErrorCode.EMAIL_EXISTED);
            }
            user.setEmail(request.getEmail());
            // Nếu đổi email mới thì cần verify lại
            user.setEmailVerified(false);
        }

        // Cập nhật các trường khác nếu có
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        // Cập nhật thời gian
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}