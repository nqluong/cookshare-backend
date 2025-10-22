package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.service.FacebookOAuthService;
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
@RequestMapping("/auth/facebook")
@RequiredArgsConstructor
@Slf4j
public class FacebookAuthController {

    private final FacebookOAuthService facebookOAuthService;

    @Value("${spring.security.oauth2.registration.facebook.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.registration.facebook.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.registration.facebook.auth-uri}")
    private String authUri;

    @Value("${spring.security.oauth2.registration.facebook.scope}")
    private String scope;

    @Value("${cookshare.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    /**
     * Endpoint để chuyển hướng người dùng đến trang đăng nhập Facebook
     */
    @GetMapping("/login")
    public ResponseEntity<Void> loginWithFacebook() {
        String authUrl = String.format(
            "%s?client_id=%s&redirect_uri=%s&scope=%s&response_type=code",
            authUri,
            clientId,
            redirectUri,
            scope.replace(",", "%2C")
        );

        return ResponseEntity.status(302)
                .location(URI.create(authUrl))
                .build();
    }

    /**
     * Callback endpoint mà Facebook sẽ redirect về sau khi user đăng nhập
     */
    @GetMapping("/callback")
    public ResponseEntity<?> facebookCallback(@RequestParam("code") String code,
                                              @RequestParam(value = "error", required = false) String error,
                                              @RequestParam(value = "error_description", required = false) String errorDescription) {
        try {
            if (error != null) {
                log.error("Facebook auth error: {} - {}", error, errorDescription);
                throw new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR);
            }

            // Xác thực với Facebook và tạo JWT tokens
            LoginResponseDTO response = facebookOAuthService.authenticateFacebookUser(code);

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
            log.error("Error during Facebook authentication: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Facebook authentication: {}", e.getMessage());
            throw new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR);
        }
    }

    /**
     * Endpoint để xử lý login từ mobile/frontend với authorization code
     */
    @PostMapping("/authenticate")
    public ResponseEntity<LoginResponseDTO> authenticateWithCode(@RequestParam("code") String code) {
        try {
            LoginResponseDTO response = facebookOAuthService.authenticateFacebookUser(code);

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
            log.error("Error authenticating with Facebook code: {}", e.getMessage());
            throw new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR);
        }
    }
}

