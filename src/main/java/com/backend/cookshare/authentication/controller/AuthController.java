package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.LoginDTO;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.dto.request.ChangePasswordRequest;
import com.backend.cookshare.authentication.service.AuthService;
import com.backend.cookshare.common.exception.CustomException;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        @Value("${cookshare.jwt.access-token-validity-in-seconds}")
        private long accessTokenExpiration;

        @Value("${cookshare.jwt.refresh-token-validity-in-seconds}")
        private long refreshTokenExpiration;

        @PostMapping("/auth/register")
        public ResponseEntity<String> register(@Valid @RequestBody UserRequest user) {
                String response = authService.register(user);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @PostMapping("/auth/login")
        public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDto) {
                LoginResponseDTO response = authService.login(loginDto);
                response.setExpiresIn(accessTokenExpiration);

                // Set cookies
                ResponseCookie resCookies = ResponseCookie
                                .from("refresh_token", response.getRefreshToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(refreshTokenExpiration)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                                .body(response);

        }

        @GetMapping("/auth/account")
        public ResponseEntity<LoginResponseDTO.UserInfo> getAccount() {
                LoginResponseDTO.UserInfo userInfo = authService.getAccount();
                return ResponseEntity.ok().body(userInfo);
        }

        @GetMapping("/auth/refresh")
        public ResponseEntity<LoginResponseDTO> getRefreshToken(
                        @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
                        @RequestHeader(name = "X-Refresh-Token", required = false) String headerRefreshToken) {

                // Ưu tiên lấy từ header (cho mobile), fallback về cookie (cho web)
                String refresh_token = headerRefreshToken != null ? headerRefreshToken : cookieRefreshToken;

                LoginResponseDTO response = authService.refreshToken(refresh_token);
                response.setExpiresIn(accessTokenExpiration);

                // set cookies
                ResponseCookie resCookies = ResponseCookie
                                .from("refresh_token", response.getRefreshToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(refreshTokenExpiration)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                                .body(response);
        }

        @PostMapping("/auth/logout")
        public ResponseEntity<String> logout(HttpServletRequest request) throws CustomException {
                authService.logout(request);

                // Xoá cookie refresh token
                ResponseCookie deleteCookie = ResponseCookie
                                .from("refresh_token", null)
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(0)
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                                .body("Đăng xuất thành công");
        }

        @PostMapping("/auth/change-password")
        public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request)
                        throws CustomException {
                authService.changePassword(request);
                return ResponseEntity.ok("Đổi mật khẩu thành công");
        }
}
