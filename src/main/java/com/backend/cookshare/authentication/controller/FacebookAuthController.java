package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.service.OAuthService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/auth/facebook")
@RequiredArgsConstructor
@Slf4j
public class FacebookAuthController {

    private final OAuthService oAuthService;

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

    @GetMapping("/login")
    public ResponseEntity<Void> loginWithFacebook(@RequestParam(required = false) String state) {
        // Nếu không có state, tạo mới
        if (state == null || state.isEmpty()) {
            state = UUID.randomUUID().toString();
            log.warn("No state provided, generated new state: {}", state);
        } else {
            log.info("Received state from client: {}", state);
        }

        String authUrl = String.format(
                "%s?client_id=%s&redirect_uri=%s&scope=%s&response_type=code&state=%s",
                authUri,
                clientId,
                redirectUri,
                scope.replace(",", "%2C"),
                state);

        log.info("Redirecting to Facebook with state: {}", state);
        log.info("Auth URL: {}", authUrl);

        return ResponseEntity.status(302)
                .location(URI.create(authUrl))
                .build();
    }

    @GetMapping("/callback")
    public Object facebookCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpServletResponse servletResponse,
            Model model) {
        try {
            log.info("Facebook callback received");
            log.info("  Code: {}", code != null ? code.substring(0, Math.min(20, code.length())) + "..." : "null");
            log.info("  State: {}", state);

            if (error != null) {
                log.error("Facebook auth error: {} - {}", error, errorDescription);
                Map<String, Object> body = Map.of(
                        "status", "error",
                        "message", error);
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body);
            }

            // Xác thực với Facebook và tạo JWT tokens (business logic in service)
            LoginResponseDTO response = oAuthService.authenticateWithOAuth(code, "facebook");

            // Lưu kết quả để polling
            oAuthService.saveAuthResult(state, response);

            // Set refresh token cookie
            ResponseCookie refreshCookie = ResponseCookie
                    .from("refresh_token", response.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(refreshTokenExpiration)
                    .build();

            servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // Trả về view template 'auth-loading'
            String s = (state == null) ? "" : state;
            model.addAttribute("state", s);
            model.addAttribute("provider", "facebook");
            return "auth-loading";

        } catch (CustomException e) {
            log.error("Error during Facebook authentication: {}", e.getMessage());

            // Lưu error để polling có thể nhận được
            oAuthService.saveAuthError(state,
                    String.valueOf(e.getErrorCode().getCode()),
                    e.getMessage());

            // Trả về HTML error page
            model.addAttribute("error", e.getMessage());
            model.addAttribute("errorCode", e.getErrorCode().getCode());
            model.addAttribute("provider", "facebook");
            model.addAttribute("state", state != null ? state : "");
            return "auth-error";
        } catch (Exception e) {
            log.error("Error during Facebook authentication: {}", e.getMessage(), e);
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
    public ResponseEntity<?> getAuthResult(@PathVariable String state) {
        log.info("Polling request for Facebook state: {}", state);

        // Check error trước
        Map<String, Object> errorResult = oAuthService.getAuthError(state);
        if (errorResult != null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResult);
        }

        // Check success result
        LoginResponseDTO result = oAuthService.getAuthResult(state);
        if (result != null) {
            return ResponseEntity.ok(result);
        }

        log.info("No Facebook result yet for state: {}", state);
        return ResponseEntity.notFound().build();
    }

    /**
     * Endpoint để xử lý login từ mobile/frontend với authorization code
     */
    @PostMapping("/authenticate")
    @ResponseBody
    public ResponseEntity<LoginResponseDTO> authenticateWithCode(@RequestParam("code") String code) {
        try {
            LoginResponseDTO response = oAuthService.authenticateWithOAuth(code, "facebook");

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
            log.error("Error during Facebook code authentication: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Facebook code authentication: {}", e.getMessage());
            throw new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR);
        }
    }
}