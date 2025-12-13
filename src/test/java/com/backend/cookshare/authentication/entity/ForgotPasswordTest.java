package com.backend.cookshare.authentication.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ForgotPasswordTest {

    @Test
    void testGetterAndSetter() {
        ForgotPassword fp = new ForgotPassword();

        UUID id = UUID.randomUUID();
        Date exp = new Date();
        User user = User.builder()
                .userId(UUID.randomUUID())
                .username("testuser")
                .passwordHash("hash")
                .build();

        fp.setFpid(id);
        fp.setOtp(123456);
        fp.setExpirationTime(exp);
        fp.setIsVerified(true);
        fp.setUser(user);
        fp.setCreatedAt(LocalDateTime.now());

        assertEquals(id, fp.getFpid());
        assertEquals(123456, fp.getOtp());
        assertEquals(exp, fp.getExpirationTime());
        assertTrue(fp.getIsVerified());
        assertEquals(user, fp.getUser());
        assertNotNull(fp.getCreatedAt());
    }

    @Test
    void testBuilder() {
        UUID id = UUID.randomUUID();
        Date exp = new Date();
        User user = User.builder()
                .userId(UUID.randomUUID())
                .username("builderUser")
                .passwordHash("pw")
                .build();

        ForgotPassword fp = ForgotPassword.builder()
                .fpid(id)
                .otp(999999)
                .expirationTime(exp)
                .isVerified(false)
                .user(user)
                .build();

        assertEquals(id, fp.getFpid());
        assertEquals(999999, fp.getOtp());
        assertEquals(exp, fp.getExpirationTime());
        assertFalse(fp.getIsVerified());
        assertEquals(user, fp.getUser());
    }

    @Test
    void testDefaultIsVerified() {
        ForgotPassword fp = ForgotPassword.builder().build();
        assertFalse(fp.getIsVerified());
    }

    @Test
    void testPrePersist() {
        ForgotPassword fp = new ForgotPassword();
        assertNull(fp.getCreatedAt());

        fp.onCreate();

        assertNotNull(fp.getCreatedAt());
        assertTrue(fp.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testUserRelation() {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .username("hello")
                .passwordHash("pw")
                .build();

        ForgotPassword fp = new ForgotPassword();
        fp.setUser(user);

        assertNotNull(fp.getUser());
        assertEquals("hello", fp.getUser().getUsername());
    }
}
