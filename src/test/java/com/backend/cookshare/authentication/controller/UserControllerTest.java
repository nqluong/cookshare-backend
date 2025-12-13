package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.UserProfileDto;
import com.backend.cookshare.authentication.dto.request.AvatarUploadUrlRequest;
import com.backend.cookshare.authentication.dto.request.UpdateUserProfileRequest;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.dto.response.AvatarUploadUrlResponse;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.UserProfileService;
import com.backend.cookshare.authentication.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Mock
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock SecurityContext cho các endpoint @PreAuthorize
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", List.of(() -> "USER"))
        );
    }

    // ===================== Create User =====================
    @Test
    void createUser_ShouldReturnCreatedMessage() {
        UserRequest userRequest = new UserRequest();
        userRequest.setUsername("newUser");
        when(userService.createUser(userRequest)).thenReturn("User created successfully");

        ResponseEntity<String> response = userController.createUser(userRequest);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals("User created successfully", response.getBody());
    }

    // ===================== Get User by ID =====================
    @Test
    void getUserById_UserExists_ShouldReturnUserProfile() {
        UUID userId = UUID.randomUUID();
        UserProfileDto dto = new UserProfileDto();
        dto.setUsername("testuser");

        when(userProfileService.getUserProfileById(userId)).thenReturn(Optional.of(dto));

        ResponseEntity<UserProfileDto> response = userController.getUserById(userId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("testuser", response.getBody().getUsername());
    }

    @Test
    void getUserById_UserNotFound_ShouldReturn404() {
        UUID userId = UUID.randomUUID();

        when(userProfileService.getUserProfileById(userId)).thenReturn(Optional.empty());

        ResponseEntity<UserProfileDto> response = userController.getUserById(userId);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    // ===================== Get User by Username =====================
    @Test
    void getUserByUsername_UserExists_ShouldReturnUserProfile() {
        String username = "testuser";
        UserProfileDto dto = new UserProfileDto();
        dto.setUsername(username);

        when(userProfileService.getUserProfileByUsername(username)).thenReturn(Optional.of(dto));

        ResponseEntity<UserProfileDto> response = userController.getUserByUsername(username);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(username, response.getBody().getUsername());
    }

    @Test
    void getUserByUsername_UserNotFound_ShouldReturn404() {
        String username = "unknown";
        when(userProfileService.getUserProfileByUsername(username)).thenReturn(Optional.empty());

        ResponseEntity<UserProfileDto> response = userController.getUserByUsername(username);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    // ===================== Get User by Email =====================
    @Test
    void getUserByEmail_UserExists_ShouldReturnUser() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);

        when(userService.getUserByEmail(email)).thenReturn(Optional.of(user));

        ResponseEntity<User> response = userController.getUserByEmail(email);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(email, response.getBody().getEmail());
    }

    @Test
    void getUserByEmail_UserNotFound_ShouldReturn404() {
        String email = "unknown@example.com";
        when(userService.getUserByEmail(email)).thenReturn(Optional.empty());

        ResponseEntity<User> response = userController.getUserByEmail(email);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    // ===================== Get All Users =====================
    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        List<User> users = List.of(new User(), new User());
        when(userService.getAllUsers()).thenReturn(users);

        ResponseEntity<List<User>> response = userController.getAllUsers();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
    }

    // ===================== Update User =====================
    @Test
    void updateUser_UserExists_ShouldReturnUpdatedUser() {
        UUID userId = UUID.randomUUID();
        User existingUser = new User();
        existingUser.setUserId(userId);

        User updateUser = new User();
        updateUser.setUserId(userId);

        when(userService.getUserById(userId)).thenReturn(Optional.of(existingUser));
        when(userService.updateUser(updateUser)).thenReturn(updateUser);

        ResponseEntity<User> response = userController.updateUser(userId, updateUser);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(userId, response.getBody().getUserId());
    }

    @Test
    void updateUser_UserNotFound_ShouldReturn404() {
        UUID userId = UUID.randomUUID();
        User updateUser = new User();

        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        ResponseEntity<User> response = userController.updateUser(userId, updateUser);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    // ===================== Delete User =====================
    @Test
    void deleteUser_UserExists_ShouldReturnNoContent() {
        UUID userId = UUID.randomUUID();
        User user = new User();

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userService).deleteUser(userId);

        ResponseEntity<Void> response = userController.deleteUser(userId);

        assertEquals(204, response.getStatusCodeValue());
    }

    @Test
    void deleteUser_UserNotFound_ShouldReturn404() {
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = userController.deleteUser(userId);

        assertEquals(404, response.getStatusCodeValue());
    }

    // ===================== Check Username Exists =====================
    @Test
    void checkUsernameExists_ShouldReturnTrue() {
        String username = "testuser";
        when(userService.existsByUsername(username)).thenReturn(true);

        ResponseEntity<Boolean> response = userController.checkUsernameExists(username);

        assertTrue(response.getBody());
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void checkUsernameExists_ShouldReturnFalse() {
        String username = "unknown";
        when(userService.existsByUsername(username)).thenReturn(false);

        ResponseEntity<Boolean> response = userController.checkUsernameExists(username);

        assertFalse(response.getBody());
        assertEquals(200, response.getStatusCodeValue());
    }

    // ===================== Check Email Exists =====================
    @Test
    void checkEmailExists_ShouldReturnTrue() {
        String email = "test@example.com";
        when(userService.existsByEmail(email)).thenReturn(true);

        ResponseEntity<Boolean> response = userController.checkEmailExists(email);

        assertTrue(response.getBody());
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void checkEmailExists_ShouldReturnFalse() {
        String email = "unknown@example.com";
        when(userService.existsByEmail(email)).thenReturn(false);

        ResponseEntity<Boolean> response = userController.checkEmailExists(email);

        assertFalse(response.getBody());
        assertEquals(200, response.getStatusCodeValue());
    }

    // ===================== Update User Profile =====================
    @Test
    void updateUserProfile_ShouldReturnUpdatedProfile() {
        UUID userId = UUID.randomUUID();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        UserProfileDto updatedProfile = new UserProfileDto();
        updatedProfile.setUsername("updatedUser");

        when(userProfileService.updateUserProfile(userId, request)).thenReturn(updatedProfile);

        ResponseEntity<UserProfileDto> response = userController.updateUserProfile(userId, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("updatedUser", response.getBody().getUsername());
    }

    // ===================== Avatar Upload URL =====================
    @Test
    void requestAvatarUploadUrl_ShouldReturnUrl() {
        UUID userId = UUID.randomUUID();
        AvatarUploadUrlRequest request = new AvatarUploadUrlRequest();
        AvatarUploadUrlResponse responseDto = new AvatarUploadUrlResponse();
        responseDto.setUploadUrl("http://example.com/upload");

        when(userProfileService.generateAvatarUploadUrl(userId, request)).thenReturn(responseDto);

        ResponseEntity<AvatarUploadUrlResponse> response = userController.requestAvatarUploadUrl(userId, request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("http://example.com/upload", response.getBody().getUploadUrl());
    }
}
