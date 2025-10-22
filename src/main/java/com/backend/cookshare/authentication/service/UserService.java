package com.backend.cookshare.authentication.service;

import com.backend.cookshare.authentication.dto.request.UpdateUserProfileRequest;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.entity.User;

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

    User updateUserProfile(UUID userId, UpdateUserProfileRequest request);
}
