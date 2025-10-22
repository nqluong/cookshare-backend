package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.response.FacebookTokenResponse;
import com.backend.cookshare.authentication.dto.response.FacebookUserInfo;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.enums.UserRole;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FacebookOAuthService;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookOAuthServiceImpl implements FacebookOAuthService {
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.registration.facebook.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.registration.facebook.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.registration.facebook.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.registration.facebook.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.registration.facebook.user-info-uri}")
    private String userInfoUri;

    @Override
    public FacebookTokenResponse getAccessToken(String code) {
        try {
            String tokenUrl = String.format(
                "%s?client_id=%s&client_secret=%s&redirect_uri=%s&code=%s",
                tokenUri,
                clientId,
                clientSecret,
                redirectUri,
                code
            );

            ResponseEntity<FacebookTokenResponse> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.GET,
                    null,
                    FacebookTokenResponse.class
            );

            if (response.getBody() == null) {
                log.error("Facebook token response is null");
                throw new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR);
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting access token from Facebook: {}", e.getMessage());
            throw new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR);
        }
    }

    @Override
    public FacebookUserInfo getUserInfo(String accessToken) {
        try {
            String userInfoUrl = userInfoUri + "&access_token=" + accessToken;

            ResponseEntity<FacebookUserInfo> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    null,
                    FacebookUserInfo.class
            );

            if (response.getBody() == null) {
                log.error("Facebook user info response is null");
                throw new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR);
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting user info from Facebook: {}", e.getMessage());
            throw new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR);
        }
    }

    @Override
    public LoginResponseDTO authenticateFacebookUser(String code) {
        // Lấy access token từ Facebook
        FacebookTokenResponse tokenResponse = getAccessToken(code);

        // Lấy thông tin user từ Facebook
        FacebookUserInfo facebookUserInfo = getUserInfo(tokenResponse.getAccessToken());

        // Tìm hoặc tạo user trong database
        User user = findOrCreateUser(facebookUserInfo);

        // Cập nhật last active
        user.setLastActive(LocalDateTime.now());
        userRepository.save(user);

        // Tạo JWT tokens
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .build();

        String accessToken = securityUtil.createAccessToken(user.getUsername(), userInfo);
        String refreshToken = securityUtil.createRefreshToken(user.getUsername(),
                LoginResponseDTO.builder().user(userInfo).build());

        // Update refresh token
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userInfo)
                .build();
    }

    @Override
    public User findOrCreateUser(FacebookUserInfo facebookUserInfo) {
        // Tìm user theo Facebook ID
        Optional<User> existingUser = userRepository.findByFacebookId(facebookUserInfo.getFacebookId());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Cập nhật thông tin nếu có thay đổi
            user.setFullName(facebookUserInfo.getName());
            user.setAvatarUrl(facebookUserInfo.getPictureUrl());
            user.setEmailVerified(true); // Facebook verified email
            return userRepository.save(user);
        }

        // Kiểm tra email đã tồn tại chưa (nếu Facebook cung cấp email)
        if (facebookUserInfo.getEmail() != null && !facebookUserInfo.getEmail().isEmpty()) {
            Optional<User> userByEmail = userRepository.findByEmail(facebookUserInfo.getEmail());
            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                // Link Facebook account với user hiện có
                user.setFacebookId(facebookUserInfo.getFacebookId());
                user.setAvatarUrl(facebookUserInfo.getPictureUrl());
                user.setEmailVerified(true);
                return userRepository.save(user);
            }
        }

        // Tạo user mới
        String email = facebookUserInfo.getEmail();
        if (email == null || email.isEmpty()) {
            // Nếu Facebook không cung cấp email, tạo email giả
            email = "facebook_" + facebookUserInfo.getFacebookId() + "@cookshare.local";
        }

        String username = generateUniqueUsername(email);

        User newUser = User.builder()
                .username(username)
                .email(email)
                .fullName(facebookUserInfo.getName())
                .facebookId(facebookUserInfo.getFacebookId())
                .avatarUrl(facebookUserInfo.getPictureUrl())
                .passwordHash("FACEBOOK_AUTH") // Không cần password cho Facebook login
                .role(UserRole.USER)
                .isActive(true)
                .emailVerified(true)
                .lastActive(LocalDateTime.now())
                .followerCount(0)
                .followingCount(0)
                .recipeCount(0)
                .build();

        return userRepository.save(newUser);
    }

    @Override
    public String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;

        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}

