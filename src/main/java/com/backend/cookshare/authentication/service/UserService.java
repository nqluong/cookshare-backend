package com.backend.cookshare.authentication.service;

import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    String createUser(UserRequest user);

    Optional<User> getUserById(UUID userId);

    Optional<User> getUserByUsername(String username);

    Optional<User> getUserByEmail(String email);

    Optional<User> getUserByUsernameOrEmail(String usernameOrEmail);

    List<User> getAllUsers();

    User updateUser(User user);

    void deleteUser(UUID userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void updateLastActive(UUID userId);

    void updateUserToken(String token, String username);

    User getUserByRefreshTokenAndUsername(String token, String username);

    void changePassword(String username, String currentPassword, String newPassword);

    // Admin methods
    Page<User> getAllUsersWithPagination(String search, Pageable pageable);

    User getUserDetailById(UUID userId);

    void banUser(UUID userId, String reason);

    void unbanUser(UUID userId);

    void deleteUserByAdmin(UUID userId);
}
