package com.backend.cookshare.authentication.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Service
public class TokenBlacklistService {

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    /**
     * Thêm token vào blacklist
     */
    public void blacklistToken(String token) {
        if (token != null && !token.isEmpty()) {
            blacklistedTokens.add(token);
        }
    }

    /**
     * Kiểm tra token có bị blacklist không
     */
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    /**
     * Xóa token khỏi blacklist (có thể dùng cho cleanup)
     */
    public void removeFromBlacklist(String token) {
        blacklistedTokens.remove(token);
    }

    /**
     * Lấy số lượng token trong blacklist
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
}
