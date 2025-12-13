package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.enums.UserRole;
import com.backend.cookshare.authentication.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsCustomTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserDetailsCustom userDetailsCustom;

    private String username;
    private String email;
    private String passwordHash;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        username = "testuser";
        email = "test@example.com";
        passwordHash = "encodedPassword123";
        userId = UUID.randomUUID();

        user = User.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .fullName("Test User")
                .role(UserRole.USER)
                .isActive(true)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .lastActive(LocalDateTime.now())
                .followerCount(0)
                .followingCount(0)
                .recipeCount(0)
                .build();
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails() {
        // Arrange
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsCustom.loadUserByUsername(username);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(passwordHash, result.getPassword());
        assertNotNull(result.getAuthorities());
        assertTrue(result.getAuthorities().isEmpty());
        verify(userService).getUserByUsername(username);
    }

    @Test
    void loadUserByUsername_UserNotFound_ShouldThrowException() {
        // Arrange
        when(userService.getUserByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsCustom.loadUserByUsername(username);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userService).getUserByUsername(username);
    }

    @Test
    void loadUserByUsername_WithDifferentUsername_ShouldReturnCorrectUser() {
        // Arrange
        String differentUsername = "anotheruser";
        User differentUser = User.builder()
                .userId(UUID.randomUUID())
                .username(differentUsername)
                .email("another@example.com")
                .passwordHash("anotherPassword")
                .fullName("Another User")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userService.getUserByUsername(differentUsername)).thenReturn(Optional.of(differentUser));

        // Act
        UserDetails result = userDetailsCustom.loadUserByUsername(differentUsername);

        // Assert
        assertNotNull(result);
        assertEquals(differentUsername, result.getUsername());
        assertEquals("anotherPassword", result.getPassword());
        verify(userService).getUserByUsername(differentUsername);
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetailsWithEmptyAuthorities() {
        // Arrange
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsCustom.loadUserByUsername(username);

        // Assert
        assertNotNull(result.getAuthorities());
        assertEquals(0, result.getAuthorities().size());
        assertTrue(result.getAuthorities().isEmpty());
    }

    @Test
    void loadUserByUsername_WithNullUsername_ShouldThrowException() {
        // Arrange
        when(userService.getUserByUsername(null)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsCustom.loadUserByUsername(null);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userService).getUserByUsername(null);
    }

    @Test
    void loadUserByUsername_WithEmptyUsername_ShouldThrowException() {
        // Arrange
        String emptyUsername = "";
        when(userService.getUserByUsername(emptyUsername)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsCustom.loadUserByUsername(emptyUsername);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userService).getUserByUsername(emptyUsername);
    }

    @Test
    void loadUserByUsername_WithInactiveUser_ShouldStillReturnUserDetails() {
        // Arrange
        user.setIsActive(false);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsCustom.loadUserByUsername(username);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(passwordHash, result.getPassword());
        verify(userService).getUserByUsername(username);
    }

    @Test
    void loadUserByUsername_WithUnverifiedEmail_ShouldStillReturnUserDetails() {
        // Arrange
        user.setEmailVerified(false);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsCustom.loadUserByUsername(username);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(passwordHash, result.getPassword());
        verify(userService).getUserByUsername(username);
    }

    @Test
    void loadUserByUsername_WithAdminRole_ShouldReturnUserDetails() {
        // Arrange
        user.setRole(UserRole.ADMIN);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsCustom.loadUserByUsername(username);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(passwordHash, result.getPassword());
        verify(userService).getUserByUsername(username);
    }

    @Test
    void loadUserByUsername_MultipleCallsWithSameUsername_ShouldCallServiceEachTime() {
        // Arrange
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        // Act
        userDetailsCustom.loadUserByUsername(username);
        userDetailsCustom.loadUserByUsername(username);
        userDetailsCustom.loadUserByUsername(username);

        // Assert
        verify(userService, times(3)).getUserByUsername(username);
    }

    @Test
    void loadUserByUsername_ShouldReturnSpringSecurityUserInstance() {
        // Arrange
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsCustom.loadUserByUsername(username);

        // Assert
        assertNotNull(result);
        assertInstanceOf(org.springframework.security.core.userdetails.User.class, result);
    }

    @Test
    void loadUserByUsername_WithSpecialCharactersInUsername_ShouldWork() {
        // Arrange
        String specialUsername = "user@test.com";
        User specialUser = User.builder()
                .userId(UUID.randomUUID())
                .username(specialUsername)
                .email(specialUsername)
                .passwordHash("password")
                .fullName("Special User")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userService.getUserByUsername(specialUsername)).thenReturn(Optional.of(specialUser));

        // Act
        UserDetails result = userDetailsCustom.loadUserByUsername(specialUsername);

        // Assert
        assertNotNull(result);
        assertEquals(specialUsername, result.getUsername());
        verify(userService).getUserByUsername(specialUsername);
    }

    @Test
    void loadUserByUsername_ShouldPreservePasswordHash() {
        // Arrange
        String complexPassword = "$2a$10$abcdefghijklmnopqrstuv";
        user.setPasswordHash(complexPassword);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsCustom.loadUserByUsername(username);

        // Assert
        assertEquals(complexPassword, result.getPassword());
    }

    @Test
    void constructor_ShouldInitializeWithUserService() {
        // Arrange & Act
        UserDetailsCustom customService = new UserDetailsCustom(userService);

        // Assert
        assertNotNull(customService);
    }
}