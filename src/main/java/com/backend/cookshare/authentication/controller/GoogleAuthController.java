package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.service.GoogleOAuthService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Controller
@RequestMapping("/auth/google")
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthController {

    private final GoogleOAuthService googleOAuthService;

    @Value("${spring.security.oauth2.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.registration.google.redirect-uri}")
    private String redirectUri;

    @Value("${cookshare.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";

    // Lưu trữ tạm thời kết quả đăng nhập (trong production nên dùng Redis)
    private final Map<String, LoginResponseDTO> authResults = new ConcurrentHashMap<>();

    @GetMapping("/login")
    @ResponseBody
    public ResponseEntity<Void> loginWithGoogle(@RequestParam(required = false) String state) {
        // Nếu không có state, tạo mới
        if (state == null || state.isEmpty()) {
            state = UUID.randomUUID().toString();
        }

        String scope = "openid email profile";
        String authUrl = String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&access_type=offline&prompt=consent&state=%s",
                GOOGLE_AUTH_URL,
                clientId,
                redirectUri,
                scope.replace(" ", "%20"),
                state);

        return ResponseEntity.status(302)
                .location(URI.create(authUrl))
                .build();
    }

    /**
     * Callback endpoint - Trả về HTML page để đóng browser (view: auth-loading)
     */
    @GetMapping("/callback")
    public Object googleCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse servletResponse,
            Model model) {
        try {
            if (error != null) {
                log.error("Google auth error: {}", error);
                Map<String, Object> body = Map.of(
                        "status", "error",
                        "message", error);

                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body);
            }

            // Xác thực với Google và tạo JWT tokens
            LoginResponseDTO response = googleOAuthService.authenticateGoogleUser(code);

            // Kiểm tra tài khoản có bị khóa không
            if (response.getUser() != null && !response.getUser().getIsActive()) {
                throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
            }

            // Lưu kết quả vào map với state làm key
            if (state != null && !state.isEmpty()) {
                authResults.put(state, response);
                log.info("Saved auth result for state: {}", state);

                // Tự động xóa sau 5 phút
                scheduleResultCleanup(state);
            }

            // Set refresh token cookie
            ResponseCookie refreshCookie = ResponseCookie
                    .from("refresh_token", response.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(refreshTokenExpiration)
                    .build();

            // Đưa cookie vào HttpServletResponse header
            servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // Trả về view template 'auth-loading' và truyền state
            String s = (state == null) ? "" : state;
            model.addAttribute("state", s);
            model.addAttribute("provider", "google");
            return "auth-loading";

        } catch (CustomException e) {
            log.error("Error during Google authentication: {}", e.getMessage());

            // Trả về HTML error page để browser có thể hiển thị
            model.addAttribute("error", e.getMessage());
            model.addAttribute("errorCode", e.getErrorCode().getCode());
            model.addAttribute("provider", "google");
            model.addAttribute("state", state != null ? state : "");
            return "auth-error"; // Tạo template auth-error.html
        } catch (Exception e) {
            log.error("Unexpected error during Google authentication: {}", e.getMessage());
            Map<String, Object> body = Map.of(
                    "status", "error",
                    "message", "Authentication failed");

            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        }
    }

    /**
     * Endpoint để app polling kết quả đăng nhập
     */
    @GetMapping("/result/{state}")
    @ResponseBody
    public ResponseEntity<LoginResponseDTO> getAuthResult(@PathVariable String state) {
        LoginResponseDTO result = authResults.get(state);

        if (result != null) {
            log.info("Auth result retrieved for state: {}", state);

            // Không xóa ngay, đánh dấu đã lấy và để auto-cleanup xóa sau 30s
            // Điều này tránh race condition khi frontend có nhiều request pending
            scheduleResultRemoval(state, 30000); // Xóa sau 30 giây

            return ResponseEntity.ok(result);
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * Endpoint để xử lý login từ mobile/frontend với authorization code
     */
    @PostMapping("/authenticate")
    @ResponseBody
    public ResponseEntity<LoginResponseDTO> authenticateWithCode(@RequestParam("code") String code) {
        try {
            LoginResponseDTO response = googleOAuthService.authenticateGoogleUser(code);

            // Kiểm tra tài khoản có bị khóa không
            if (response.getUser() != null && !response.getUser().getIsActive()) {
                throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
            }

            ResponseCookie refreshCookie = ResponseCookie
                    .from("refresh_token", response.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(refreshTokenExpiration)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(response);

        } catch (Exception e) {
            log.error("Error authenticating with Google code: {}", e.getMessage());
            throw new CustomException(ErrorCode.GOOGLE_AUTH_ERROR);
        }
    }

    private void scheduleResultCleanup(String state) {
        // Tự động xóa kết quả sau 5 phút để tránh memory leak
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

    private void scheduleResultRemoval(String state, long delayMillis) {
        // Xóa result sau khoảng thời gian delay (để tránh race condition)
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                authResults.remove(state);
                log.info("Removed auth result after delay for state: {}", state);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
