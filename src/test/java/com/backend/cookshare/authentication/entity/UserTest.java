package com.backend.cookshare.authentication.entity;

import com.backend.cookshare.authentication.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    void testPrePersist() {
        User user = new User();
        user.onCreate();

        assertNotNull(user.getCreatedAt(), "createdAt must be set on create");
        assertNotNull(user.getUpdatedAt(), "updatedAt must be set on create");

        assertEquals(user.getCreatedAt().getDayOfYear(), user.getUpdatedAt().getDayOfYear());
    }

    @Test
    void testPreUpdate_ShouldUpdateUpdatedAt() throws InterruptedException {
        User user = new User();
        user.onCreate();

        LocalDateTime oldUpdated = user.getUpdatedAt();

        // Đảm bảo timestamp khác milli-giây
        Thread.sleep(2);

        user.onUpdate();

        LocalDateTime newUpdated = user.getUpdatedAt();

        assertNotNull(newUpdated);
        assertNotEquals(oldUpdated, newUpdated, "updatedAt must change after update");
    }

    @Test
    void testDefaultValues() {
        User user = new User();

        assertEquals(UserRole.USER, user.getRole());
        assertEquals(0, user.getFollowerCount());
        assertEquals(0, user.getFollowingCount());
        assertEquals(0, user.getRecipeCount());
        assertTrue(user.getIsActive());
        assertFalse(user.getEmailVerified());
    }

    @Test
    void testSettersAndGetters() {
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@gmail.com");
        user.setFullName("John Doe");

        assertEquals("john", user.getUsername());
        assertEquals("john@gmail.com", user.getEmail());
        assertEquals("John Doe", user.getFullName());
    }

    @Test
    void testBuilder() {
        User user = User.builder()
                .username("alice")
                .email("alice@gmail.com")
                .passwordHash("123")
                .role(UserRole.ADMIN)
                .build();

        assertEquals("alice", user.getUsername());
        assertEquals("alice@gmail.com", user.getEmail());
        assertEquals("123", user.getPasswordHash());
        assertEquals(UserRole.ADMIN, user.getRole());
    }
}
