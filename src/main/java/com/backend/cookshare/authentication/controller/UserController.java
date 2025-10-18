package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RecipeRepository recipeRepository;

    @PostMapping
    public ResponseEntity<String> createUser(@Valid @RequestBody UserRequest user) {
        String response = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<LoginResponseDTO.UserInfo> getUserById(@PathVariable UUID userId) {
        return userService.getUserById(userId)
                .map(user -> {
                    // Lấy tổng lượt thích của user
                    int totalLikes = recipeRepository.getTotalLikeCountByUserId(user.getUserId());

                    // Map dữ liệu sang UserInfo DTO
                    LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                            .userId(user.getUserId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .role(user.getRole())
                            .isActive(user.getIsActive())
                            .emailVerified(user.getEmailVerified())
                            .build();

                    return ResponseEntity.ok(userInfo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<LoginResponseDTO.UserInfo> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> {
                    // Lấy tổng lượt thích của user
                    int totalLikes = recipeRepository.getTotalLikeCountByUserId(user.getUserId());

                    // Map dữ liệu sang UserInfo DTO
                    LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                            .userId(user.getUserId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .role(user.getRole())
                            .isActive(user.getIsActive())
                            .emailVerified(user.getEmailVerified())
                            .build();

                    return ResponseEntity.ok(userInfo);
                })
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable UUID userId, @RequestBody User user) {
        return userService.getUserById(userId)
                .map(existingUser -> {
                    user.setUserId(userId);
                    User updatedUser = userService.updateUser(user);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        if (userService.getUserById(userId).isPresent()) {
            userService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/exists/username/{username}")
    public ResponseEntity<Boolean> checkUsernameExists(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/exists/email/{email}")
    public ResponseEntity<Boolean> checkEmailExists(@PathVariable String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }
}