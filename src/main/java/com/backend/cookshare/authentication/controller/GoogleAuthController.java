package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.service.GoogleOAuthService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
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

    /**
     * Endpoint để chuyển hướng người dùng đến trang đăng nhập Google
     */
    @GetMapping("/login")
    public ResponseEntity<Void> loginWithGoogle() {
        String scope = "openid email profile";
        String authUrl = String.format(
            "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&access_type=offline&prompt=consent",
            GOOGLE_AUTH_URL,
            clientId,
            redirectUri,
            scope.replace(" ", "%20")
        );

        return ResponseEntity.status(302)
                .location(URI.create(authUrl))
                .build();
    }

    /**
     * Callback endpoint mà Google sẽ redirect về sau khi user đăng nhập
     */
    @GetMapping("/callback")
    public ResponseEntity<?> googleCallback(@RequestParam("code") String code,
                                           @RequestParam(value = "error", required = false) String error) {
        try {
            if (error != null) {
                log.error("Google auth error: {}", error);
                throw new CustomException(ErrorCode.GOOGLE_AUTH_ERROR);
            }

            // Xác thực với Google và tạo JWT tokens
            LoginResponseDTO response = googleOAuthService.authenticateGoogleUser(code);

            // Set refresh token cookie
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

        } catch (CustomException e) {
            log.error("Error during Google authentication: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Google authentication: {}", e.getMessage());
            throw new CustomException(ErrorCode.GOOGLE_AUTH_ERROR);
        }
    }

    /**
     * Endpoint để xử lý login từ mobile/frontend với authorization code
     */
    @PostMapping("/authenticate")
    public ResponseEntity<LoginResponseDTO> authenticateWithCode(@RequestParam("code") String code) {
        try {
            LoginResponseDTO response = googleOAuthService.authenticateGoogleUser(code);

            // Set refresh token cookie
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
}

