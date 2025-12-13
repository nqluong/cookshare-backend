package com.backend.cookshare.authentication.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
    }

    @Test
    void testBlacklistToken_ShouldAddValidToken() {
        tokenBlacklistService.blacklistToken("abc123");
        assertTrue(tokenBlacklistService.isTokenBlacklisted("abc123"));
        assertEquals(1, tokenBlacklistService.getBlacklistSize());
    }

    @Test
    void testBlacklistToken_ShouldIgnoreNullAndEmpty() {
        tokenBlacklistService.blacklistToken(null);
        tokenBlacklistService.blacklistToken("");

        assertEquals(0, tokenBlacklistService.getBlacklistSize());
    }

    @Test
    void testIsTokenBlacklisted_ShouldReturnFalseForNonExistentToken() {
        assertFalse(tokenBlacklistService.isTokenBlacklisted("not_exist"));
    }

    @Test
    void testRemoveFromBlacklist_ShouldRemoveToken() {
        tokenBlacklistService.blacklistToken("abc123");
        assertTrue(tokenBlacklistService.isTokenBlacklisted("abc123"));

        tokenBlacklistService.removeFromBlacklist("abc123");
        assertFalse(tokenBlacklistService.isTokenBlacklisted("abc123"));
        assertEquals(0, tokenBlacklistService.getBlacklistSize());
    }

    @Test
    void testGetBlacklistSize_ShouldReturnCorrectCount() {
        tokenBlacklistService.blacklistToken("t1");
        tokenBlacklistService.blacklistToken("t2");
        tokenBlacklistService.blacklistToken("t3");

        assertEquals(3, tokenBlacklistService.getBlacklistSize());
    }
}
