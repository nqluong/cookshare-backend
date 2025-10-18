package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.LoginDTO;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.dto.request.ChangePasswordRequest;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.authentication.service.TokenBlacklistService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import com.backend.cookshare.authentication.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final RecipeRepository recipeRepository;

    private final TokenBlacklistService tokenBlacklistService;

    @Value("${cookshare.jwt.access-token-validity-in-seconds}")
    private long accessTokenExpiration;

    @Value("${cookshare.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;


    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRequest user) {
        String response = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDto) {
        //Nạp input gồm username/password vào Security
        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

        //xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        //create a token
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Lấy thông tin user từ database
        User user = userService.getUserByUsernameOrEmail(loginDto.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Cập nhật last active
        user.setLastActive(LocalDateTime.now());
        userService.updateUser(user);

        // Tạo response với thông tin chi tiết
        LoginResponseDTO res = new LoginResponseDTO();
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .build();
        res.setUser(userInfo);

        String access_token = this.securityUtil.createAccessToken(authentication.getName(), res.getUser());
        res.setAccessToken(access_token);

        //create refresh token
        String refresh_token = this.securityUtil.createRefreshToken(loginDto.getUsername(), res);

        LoginResponseDTO response = LoginResponseDTO.builder()
                .accessToken(access_token)
                .refreshToken(refresh_token)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .user(userInfo)
                .build();

        //update user
        this.userService.updateUserToken(refresh_token, loginDto.getUsername());

        //set cookies
        ResponseCookie resCookies = ResponseCookie
                .from("refresh_token", refresh_token)
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
        String username = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get() : "";

        // Lấy thông tin user từ database
        User user = userService.getUserByUsernameOrEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .build();
        return ResponseEntity.ok().body(userInfo);
    }

    @GetMapping("/auth/refresh")
    public ResponseEntity<LoginResponseDTO> getRefreshToken(
            @CookieValue(name = "refresh_token") String refresh_token
    ) {
        //check valid
        Jwt decodedToken = this.securityUtil.checkValidRefreshToken(refresh_token);
        String username = decodedToken.getSubject();

        //check user by token and username
        User user = this.userService.getUserByRefreshTokenAndUsername(refresh_token, username);
        if (user == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        LoginResponseDTO res = new LoginResponseDTO();
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .build();
        res.setUser(userInfo);

        String access_token = this.securityUtil.createAccessToken(username, res.getUser());
        res.setAccessToken(access_token);

        //create refresh token
        String new_refresh_token = this.securityUtil.createRefreshToken(username, res);

        LoginResponseDTO response = LoginResponseDTO.builder()
                .accessToken(access_token)
                .refreshToken(new_refresh_token)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .user(userInfo)
                .build();

        //update user
        this.userService.updateUserToken(new_refresh_token, username);

        //set cookies
        ResponseCookie resCookies = ResponseCookie
                .from("refresh_token", new_refresh_token)
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
        // Lấy access token từ header
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        accessToken = accessToken.substring(7);

        try {
            // Validate access token
            Jwt decodedToken = this.securityUtil.checkValidAccessToken(accessToken);
            String username = decodedToken.getSubject();

            if (username == null || username.isEmpty()) {
                throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
            }

            // Kiểm tra user có tồn tại không
            User user = this.userService.getUserByUsernameOrEmail(username)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // Đưa access token vào blacklist
            tokenBlacklistService.blacklistToken(accessToken);

            // Xoá refresh token trong database
            this.userService.updateUserToken(null, username);

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

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    @PostMapping("/auth/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) throws CustomException {
        // Lấy username từ JWT token
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_ACCESS_TOKEN));

        // Kiểm tra mật khẩu mới và xác nhận mật khẩu có khớp không
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // Gọi service để đổi mật khẩu
        userService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
}
