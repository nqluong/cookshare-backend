package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.GoogleOAuthService;
import com.backend.cookshare.authentication.service.FacebookOAuthService;
import com.backend.cookshare.authentication.service.OAuthService;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OAuthServiceImpl implements OAuthService {

    private final GoogleOAuthService googleOAuthService;
    private final FacebookOAuthService facebookOAuthService;
    private final UserService userService;
    private final SecurityUtil securityUtil;

    // Lưu trữ tạm thời kết quả đăng nhập (trong production nên dùng Redis)
    private final Map<String, LoginResponseDTO> authResults = new ConcurrentHashMap<>();

    // Lưu trữ error results riêng
    private final Map<String, Map<String, Object>> authErrors = new ConcurrentHashMap<>();

    @Override
    public LoginResponseDTO authenticateWithOAuth(String code, String provider) {
        log.info("Authenticating with {} using code", provider);

        LoginResponseDTO response;

        // Xác thực với OAuth provider
        if ("google".equalsIgnoreCase(provider)) {
            response = googleOAuthService.authenticateGoogleUser(code);
        } else if ("facebook".equalsIgnoreCase(provider)) {
            response = facebookOAuthService.authenticateFacebookUser(code);
        } else {
            throw new CustomException(ErrorCode.INVALID_OAUTH_PROVIDER);
        }

        // Kiểm tra tài khoản có bị khóa không
        if (response.getUser() != null && !response.getUser().getIsActive()) {
            log.warn("Login attempt for inactive user: {}", response.getUser().getUsername());
            throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
        }

        // Cập nhật last active
        if (response.getUser() != null && response.getUser().getUserId() != null) {
            userService.getUserById(response.getUser().getUserId()).ifPresent(user -> {
                user.setLastActive(LocalDateTime.now());
                userService.updateUser(user);
            });
        }

        log.info("{} authentication successful for user: {}", provider,
                response.getUser() != null ? response.getUser().getUsername() : "unknown");

        return response;
    }

    @Override
    public void saveAuthResult(String state, LoginResponseDTO result) {
        if (state != null && !state.isEmpty()) {
            authResults.put(state, result);
            log.info("Saved auth result for state: {}", state);

            // Tự động xóa sau 5 phút để tránh memory leak
            scheduleResultCleanup(state);
        } else {
            log.warn("Cannot save auth result: state is null or empty");
        }
    }

    @Override
    public void saveAuthError(String state, String errorCode, String errorMessage) {
        if (state != null && !state.isEmpty()) {
            Map<String, Object> errorData = Map.of(
                    "status", "error",
                    "code", errorCode,
                    "message", errorMessage);
            authErrors.put(state, errorData);
            log.info("Saved error result for state: {}", state);

            // Tự động xóa sau 5 phút
            scheduleErrorCleanup(state);
        } else {
            log.warn("Cannot save auth error: state is null or empty");
        }
    }

    @Override
    public LoginResponseDTO getAuthResult(String state) {
        LoginResponseDTO result = authResults.get(state);

        if (result != null) {
            log.info("Auth result retrieved for state: {}", state);
            // Không xóa ngay, đánh dấu đã lấy và để auto-cleanup xóa sau 30s
            scheduleResultRemoval(state, 30000);
        } else {
            log.debug("No auth result found for state: {}", state);
        }

        return result;
    }

    @Override
    public Map<String, Object> getAuthError(String state) {
        Map<String, Object> errorResult = authErrors.get(state);

        if (errorResult != null) {
            log.info("Error result retrieved for state: {}", state);
            authErrors.remove(state); // Xóa ngay sau khi lấy
        }

        return errorResult;
    }

    @Override
    @Async
    public void scheduleResultRemoval(String state, long delayMillis) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                authResults.remove(state);
                log.info("Removed auth result after delay for state: {}", state);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Result removal interrupted for state: {}", state);
            }
        }).start();
    }

    /**
     * Tự động xóa kết quả sau 5 phút để tránh memory leak
     */
    @Async
    private void scheduleResultCleanup(String state) {
        new Thread(() -> {
            try {
                Thread.sleep(5 * 60 * 1000); // 5 phút
                authResults.remove(state);
                log.info("Auto-cleaned auth result for state: {}", state);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Tự động xóa error sau 5 phút
     */
    @Async
    private void scheduleErrorCleanup(String state) {
        new Thread(() -> {
            try {
                Thread.sleep(5 * 60 * 1000); // 5 phút
                authErrors.remove(state);
                log.info("Auto-cleaned error result for state: {}", state);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}

