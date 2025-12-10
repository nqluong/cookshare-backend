package com.backend.cookshare.authentication.service;

import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;

public interface OAuthService {
    /**
     * Xác thực user với OAuth provider và tạo JWT tokens
     */
    LoginResponseDTO authenticateWithOAuth(String code, String provider);

    /**
     * Lưu kết quả authentication để polling
     */
    void saveAuthResult(String state, LoginResponseDTO result);

    /**
     * Lưu error result để polling
     */
    void saveAuthError(String state, String errorCode, String errorMessage);

    /**
     * Lấy kết quả authentication theo state
     */
    LoginResponseDTO getAuthResult(String state);

    /**
     * Lấy error result theo state
     */
    java.util.Map<String, Object> getAuthError(String state);

    /**
     * Xóa result sau một khoảng thời gian
     */
    void scheduleResultRemoval(String state, long delayMillis);
}

