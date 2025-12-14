package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.LoginDTO;
import com.backend.cookshare.authentication.dto.request.ChangePasswordRequest;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.AuthService;
import com.backend.cookshare.authentication.service.TokenBlacklistService;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public String register(UserRequest userRequest) {
        log.info("Registering new user: {}", userRequest.getUsername());
        return userService.createUser(userRequest);
    }

    @Override
    public LoginResponseDTO login(LoginDTO loginDto) {
        log.info("User login attempt: {}", loginDto.getUsername());

        // Xác thực người dùng
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(), loginDto.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(authenticationToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Lấy thông tin user từ database
        User user = userService.getUserByUsernameOrEmail(loginDto.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra tài khoản có bị khóa không
        if (!user.getIsActive()) {
            LocalDateTime now = LocalDateTime.now();

            // Trường hợp 1: Tạm khóa (có suspendedUntil)
            if (user.getSuspendedUntil() != null) {
                if (now.isAfter(user.getSuspendedUntil())) {
                    // Đã hết thời gian tạm khóa → tự động mở khóa
                    log.info("Auto-unsuspending user {} - suspension period ended", user.getUsername());
                    user.setIsActive(true);
                    user.setSuspendedUntil(null);
                    userService.updateUser(user);
                } else {
                    // Vẫn trong thời gian tạm khóa
                    long daysRemaining = java.time.Duration.between(now, user.getSuspendedUntil()).toDays() + 1;
                    log.warn("Login attempt for suspended user: {} ({} days remaining)",
                            user.getUsername(), daysRemaining);
                    String message = String.format("Tài khoản của bạn đã bị tạm khóa. Còn %d ngày nữa sẽ tự động mở khóa.",
                            daysRemaining);
                    throw new CustomException(ErrorCode.USER_NOT_ACTIVE, message);
                }
            }
            // Trường hợp 2: Cấm vĩnh viễn (có bannedAt nhưng không có suspendedUntil)
            else if (user.getBannedAt() != null) {
                log.warn("Login attempt for permanently banned user: {} (banned at {})",
                        user.getUsername(), user.getBannedAt());
                throw new CustomException(ErrorCode.USER_NOT_ACTIVE, 
                        "Tài khoản của bạn đã bị cấm vĩnh viễn do vi phạm nghiêm trọng chính sách cộng đồng.");
            }
            // Trường hợp 3: Tài khoản không hoạt động vì lý do khác
            else {
                log.warn("Login attempt for inactive user: {}", user.getUsername());
                throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
            }
        }

        // Cập nhật last active
        user.setLastActive(LocalDateTime.now());
        userService.updateUser(user);

        // Tạo UserInfo
        LoginResponseDTO.UserInfo userInfo = buildUserInfo(user);

        // Tạo tokens
        String accessToken = securityUtil.createAccessToken(authentication.getName(), userInfo);
        String refreshToken = securityUtil.createRefreshToken(loginDto.getUsername(),
                LoginResponseDTO.builder().user(userInfo).build());

        // Cập nhật refresh token trong database
        userService.updateUserToken(refreshToken, loginDto.getUsername());

        log.info("User logged in successfully: {}", user.getUsername());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userInfo)
                .build();
    }

    @Override
    public LoginResponseDTO.UserInfo getAccount() {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_ACCESS_TOKEN));

        User user = userService.getUserByUsernameOrEmail(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return buildUserInfo(user);
    }

    @Override
    public LoginResponseDTO refreshToken(String refreshToken) {
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        log.info("Refreshing token");

        // Validate refresh token
        Jwt decodedToken = securityUtil.checkValidRefreshToken(refreshToken);
        String username = decodedToken.getSubject();

        // Kiểm tra user và token
        User user = userService.getUserByRefreshTokenAndUsername(refreshToken, username);
        if (user == null) {
            log.warn("Invalid refresh token for user: {}", username);
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Tạo UserInfo
        LoginResponseDTO.UserInfo userInfo = buildUserInfo(user);

        // Tạo tokens mới
        String newAccessToken = securityUtil.createAccessToken(username, userInfo);
        String newRefreshToken = securityUtil.createRefreshToken(username,
                LoginResponseDTO.builder().user(userInfo).build());

        // Cập nhật refresh token trong database
        userService.updateUserToken(newRefreshToken, username);

        log.info("Token refreshed successfully for user: {}", username);

        return LoginResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .user(userInfo)
                .build();
    }

    @Override
    public void logout(HttpServletRequest request) {
        log.info("User logout attempt");

        // Lấy access token từ header
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        accessToken = accessToken.substring(7);

        // Validate access token
        Jwt decodedToken = securityUtil.checkValidAccessToken(accessToken);
        String username = decodedToken.getSubject();

        if (username == null || username.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        // Kiểm tra user có tồn tại không
        userService.getUserByUsernameOrEmail(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Đưa access token vào blacklist
        tokenBlacklistService.blacklistToken(accessToken);

        // Xoá refresh token trong database
        userService.updateUserToken(null, username);

        log.info("User logged out successfully: {}", username);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        // Lấy username từ JWT token
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_ACCESS_TOKEN));

        // Kiểm tra mật khẩu mới và xác nhận mật khẩu có khớp không
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        log.info("Changing password for user: {}", username);

        // Gọi service để đổi mật khẩu
        userService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());

        log.info("Password changed successfully for user: {}", username);
    }

    /**
     * Helper method để build UserInfo từ User entity
     */
    private LoginResponseDTO.UserInfo buildUserInfo(User user) {
        return LoginResponseDTO.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .followingCount(user.getFollowingCount())
                .followerCount(user.getFollowerCount())
                .recipeCount(user.getRecipeCount())
                .build();
    }
}
