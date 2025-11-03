package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.response.GoogleTokenResponse;
import com.backend.cookshare.authentication.dto.response.GoogleUserInfo;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.enums.UserRole;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.GoogleOAuthService;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthServiceImpl implements GoogleOAuthService {
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final RestTemplate restTemplate;
    private final FirebaseStorageService firebaseStorageService;

    @Value("${spring.security.oauth2.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.registration.google.redirect-uri}")
    private String redirectUri;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Override
    public GoogleTokenResponse getAccessToken(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<GoogleTokenResponse> response = restTemplate.exchange(
                    TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    GoogleTokenResponse.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting access token from Google: {}", e.getMessage());
            throw new CustomException(ErrorCode.GOOGLE_AUTH_ERROR);
        }
    }

    @Override
    public GoogleUserInfo getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
                    USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    GoogleUserInfo.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting user info from Google: {}", e.getMessage());
            throw new CustomException(ErrorCode.GOOGLE_AUTH_ERROR);
        }
    }

    @Override
    public LoginResponseDTO authenticateGoogleUser(String code) {
        // Lấy access token từ Google
        GoogleTokenResponse tokenResponse = getAccessToken(code);

        // Lấy thông tin user từ Google
        GoogleUserInfo googleUserInfo = getUserInfo(tokenResponse.getAccessToken());

        // Tìm hoặc tạo user trong database
        User user = findOrCreateUser(googleUserInfo);

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
                .avatarUrl(user.getAvatarUrl()) // Thêm avatarUrl từ Firebase
                .bio(user.getBio()) // Thêm bio
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
    public User findOrCreateUser(GoogleUserInfo googleUserInfo) {
        // Tìm user theo Google ID
        Optional<User> existingUser = userRepository.findByGoogleId(googleUserInfo.getGoogleId());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Cập nhật thông tin nếu có thay đổi
            user.setFullName(googleUserInfo.getName());

            // Tải avatar từ Google và upload lên Firebase Storage
            if (googleUserInfo.getPicture() != null && !googleUserInfo.getPicture().isEmpty()) {
                // Xóa avatar cũ trước khi upload avatar mới
                String oldAvatarUrl = user.getAvatarUrl();
                if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                    log.info("🗑️ Xóa avatar cũ của Google user trước khi cập nhật: {}", oldAvatarUrl);
                    firebaseStorageService.deleteAvatarByUrl(oldAvatarUrl);
                }

                String firebaseAvatarUrl = uploadAvatarToFirebase(
                        googleUserInfo.getPicture(),
                        user.getUserId());
                if (firebaseAvatarUrl != null) {
                    user.setAvatarUrl(firebaseAvatarUrl);
                }
            }

            user.setEmailVerified(googleUserInfo.getEmailVerified());
            return userRepository.save(user);
        }

        // Kiểm tra email đã tồn tại chưa
        Optional<User> userByEmail = userRepository.findByEmail(googleUserInfo.getEmail());
        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            // Link Google account với user hiện có
            user.setGoogleId(googleUserInfo.getGoogleId());

            // Tải avatar từ Google và upload lên Firebase Storage
            if (googleUserInfo.getPicture() != null && !googleUserInfo.getPicture().isEmpty()) {
                // Xóa avatar cũ trước khi upload avatar mới
                String oldAvatarUrl = user.getAvatarUrl();
                if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                    log.info("🗑️ Xóa avatar cũ khi link Google account: {}", oldAvatarUrl);
                    firebaseStorageService.deleteAvatarByUrl(oldAvatarUrl);
                }

                String firebaseAvatarUrl = uploadAvatarToFirebase(
                        googleUserInfo.getPicture(),
                        user.getUserId());
                if (firebaseAvatarUrl != null) {
                    user.setAvatarUrl(firebaseAvatarUrl);
                }
            }

            user.setEmailVerified(true);
            return userRepository.save(user);
        }

        // Tạo user mới
        String username = generateUniqueUsername(googleUserInfo.getEmail());

        // Tạo user mới (chưa có userId, sẽ tạo sau khi save)
        User newUser = User.builder()
                .username(username)
                .email(googleUserInfo.getEmail())
                .fullName(googleUserInfo.getName())
                .googleId(googleUserInfo.getGoogleId())
                .avatarUrl(null) // Sẽ cập nhật sau khi upload lên Firebase
                .passwordHash("GOOGLE_AUTH") // Không cần password cho Google login
                .role(UserRole.USER)
                .isActive(true)
                .emailVerified(googleUserInfo.getEmailVerified())
                .lastActive(LocalDateTime.now())
                .followerCount(0)
                .followingCount(0)
                .recipeCount(0)
                .build();

        // Save user để có userId
        newUser = userRepository.save(newUser);

        // Sau khi có userId, tải avatar từ Google và upload lên Firebase
        if (googleUserInfo.getPicture() != null && !googleUserInfo.getPicture().isEmpty()) {
            String firebaseAvatarUrl = uploadAvatarToFirebase(
                    googleUserInfo.getPicture(),
                    newUser.getUserId());
            if (firebaseAvatarUrl != null) {
                newUser.setAvatarUrl(firebaseAvatarUrl);
                newUser = userRepository.save(newUser);
            }
        }

        return newUser;
    }

    /**
     * Tải ảnh từ URL của Google và upload lên Firebase Storage
     * 
     * @param imageUrl URL ảnh từ Google
     * @param userId   ID của user
     * @return Public URL trên Firebase Storage hoặc null nếu thất bại
     */
    private String uploadAvatarToFirebase(String imageUrl, UUID userId) {
        try {
            log.info("📥 Bắt đầu tải avatar từ Google: {}", imageUrl);

            // Tải ảnh từ URL của Google
            URL url = new URL(imageUrl);
            InputStream inputStream = url.openStream();
            byte[] imageBytes = inputStream.readAllBytes();
            inputStream.close();

            log.info("✅ Đã tải {} bytes từ Google", imageBytes.length);

            // Tạo tên file unique
            String fileName = "oauth_google_" + userId + "_" + System.currentTimeMillis() + ".jpg";

            // Upload lên Firebase Storage
            firebaseStorageService.uploadFile(fileName, imageBytes, "image/jpeg");
            log.info("✅ Đã upload avatar lên Firebase Storage: {}", fileName);

            // Lấy public URL
            String publicUrl = firebaseStorageService.getPublicUrl(fileName);
            log.info("✅ Firebase avatar URL: {}", publicUrl);

            return publicUrl;

        } catch (IOException e) {
            log.error("❌ Lỗi khi tải/upload avatar từ Google lên Firebase: {}", e.getMessage(), e);
            return null;
        }
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
