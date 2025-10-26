package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@Slf4j
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
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra mật khẩu hiện tại có đúng không
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // Kiểm tra mật khẩu mới có trùng với mật khẩu hiện tại không
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new CustomException(ErrorCode.SAME_PASSWORD);
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ==================== ADMIN METHODS ====================

    @Override
    public Page<User> getAllUsersWithPagination(String search, Pageable pageable) {
        log.info("Admin fetching users with search: {}, page: {}, size: {}", 
                search, pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAllWithSearch(search, pageable);
    }

    @Override
    public User getUserDetailById(UUID userId) {
        log.info("Admin fetching user details for userId: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public void banUser(UUID userId, String reason) {
        log.info("Admin banning user: {} with reason: {}", userId, reason);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new CustomException(ErrorCode.USER_ALREADY_BANNED);
        }
        
        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User {} has been banned successfully", userId);
    }

    @Override
    public void unbanUser(UUID userId) {
        log.info("Admin unbanning user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new CustomException(ErrorCode.USER_ALREADY_ACTIVE);
        }
        
        user.setIsActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User {} has been unbanned successfully", userId);
    }

    @Override
    public void deleteUserByAdmin(UUID userId) {
        log.info("Admin deleting user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // You might want to add additional checks here, like preventing deletion of other admins
        // or performing soft delete instead of hard delete
        userRepository.delete(user);
        
        log.info("User {} has been deleted successfully", userId);
    }
}