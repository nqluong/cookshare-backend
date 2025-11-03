package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.response.FacebookTokenResponse;
import com.backend.cookshare.authentication.dto.response.FacebookUserInfo;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.enums.UserRole;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FacebookOAuthService;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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
public class FacebookOAuthServiceImpl implements FacebookOAuthService {
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final RestTemplate restTemplate;
    private final FirebaseStorageService firebaseStorageService;

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
            String url = String.format(
                    "%s?client_id=%s&client_secret=%s&redirect_uri=%s&code=%s",
                    tokenUri,
                    clientId,
                    clientSecret,
                    redirectUri,
                    code);

            ResponseEntity<FacebookTokenResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    FacebookTokenResponse.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting access token from Facebook: {}", e.getMessage());
            throw new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR);
        }
    }

    @Override
    public FacebookUserInfo getUserInfo(String accessToken) {
        try {
            String url = userInfoUri + "&access_token=" + accessToken;

            ResponseEntity<FacebookUserInfo> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    FacebookUserInfo.class);

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
    public User findOrCreateUser(FacebookUserInfo facebookUserInfo) {
        // Tìm user theo Facebook ID
        Optional<User> existingUser = userRepository.findByFacebookId(facebookUserInfo.getFacebookId());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Cập nhật thông tin nếu có thay đổi
            user.setFullName(facebookUserInfo.getName());

            // Tải avatar từ Facebook và upload lên Firebase Storage
            if (facebookUserInfo.getPictureUrl() != null && !facebookUserInfo.getPictureUrl().isEmpty()) {
                // Xóa avatar cũ trước khi upload avatar mới
                String oldAvatarUrl = user.getAvatarUrl();
                if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                    log.info("🗑️ Xóa avatar cũ của Facebook user trước khi cập nhật: {}", oldAvatarUrl);
                    firebaseStorageService.deleteAvatarByUrl(oldAvatarUrl);
                }

                String firebaseAvatarUrl = uploadAvatarToFirebase(
                        facebookUserInfo.getPictureUrl(),
                        user.getUserId());
                if (firebaseAvatarUrl != null) {
                    user.setAvatarUrl(firebaseAvatarUrl);
                }
            }
            return userRepository.save(user);
        }

        // Kiểm tra email đã tồn tại chưa (nếu Facebook cung cấp email)
        if (facebookUserInfo.getEmail() != null && !facebookUserInfo.getEmail().isEmpty()) {
            Optional<User> userByEmail = userRepository.findByEmail(facebookUserInfo.getEmail());
            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                // Link Facebook account với user hiện có
                user.setFacebookId(facebookUserInfo.getFacebookId());

                // Tải avatar từ Facebook và upload lên Firebase Storage
                if (facebookUserInfo.getPictureUrl() != null && !facebookUserInfo.getPictureUrl().isEmpty()) {
                    // Xóa avatar cũ trước khi upload avatar mới
                    String oldAvatarUrl = user.getAvatarUrl();
                    if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                        log.info("🗑️ Xóa avatar cũ khi link Facebook account: {}", oldAvatarUrl);
                        firebaseStorageService.deleteAvatarByUrl(oldAvatarUrl);
                    }

                    String firebaseAvatarUrl = uploadAvatarToFirebase(
                            facebookUserInfo.getPictureUrl(),
                            user.getUserId());
                    if (firebaseAvatarUrl != null) {
                        user.setAvatarUrl(firebaseAvatarUrl);
                    }
                }

                user.setEmailVerified(true);
                return userRepository.save(user);
            }
        }

        // Tạo user mới
        String email = facebookUserInfo.getEmail();
        String username;

        // Nếu Facebook không cung cấp email, tạo email giả từ Facebook ID
        if (email == null || email.isEmpty()) {
            email = "facebook_" + facebookUserInfo.getFacebookId() + "@cookshare.app";
            username = "facebook_" + facebookUserInfo.getFacebookId();
        } else {
            username = generateUniqueUsername(email);
        }

        username = generateUniqueUsername(username);

        User newUser = User.builder()
                .username(username)
                .email(email)
                .fullName(facebookUserInfo.getName())
                .facebookId(facebookUserInfo.getFacebookId())
                .avatarUrl(null) // Sẽ cập nhật sau khi upload lên Firebase
                .passwordHash("FACEBOOK_AUTH") // Không cần password cho Facebook login
                .role(UserRole.USER)
                .isActive(true)
                .emailVerified(facebookUserInfo.getEmail() != null && !facebookUserInfo.getEmail().isEmpty())
                .lastActive(LocalDateTime.now())
                .followerCount(0)
                .followingCount(0)
                .recipeCount(0)
                .build();

        // Save user để có userId
        newUser = userRepository.save(newUser);

        // Sau khi có userId, tải avatar từ Facebook và upload lên Firebase
        if (facebookUserInfo.getPictureUrl() != null && !facebookUserInfo.getPictureUrl().isEmpty()) {
            String firebaseAvatarUrl = uploadAvatarToFirebase(
                    facebookUserInfo.getPictureUrl(),
                    newUser.getUserId());
            if (firebaseAvatarUrl != null) {
                newUser.setAvatarUrl(firebaseAvatarUrl);
                newUser = userRepository.save(newUser);
            }
        }

        return newUser;
    }

    /**
     * Tải ảnh từ URL của Facebook và upload lên Firebase Storage
     * 
     * @param imageUrl URL ảnh từ Facebook
     * @param userId   ID của user
     * @return Public URL trên Firebase Storage hoặc null nếu thất bại
     */
    private String uploadAvatarToFirebase(String imageUrl, UUID userId) {
        try {
            log.info("📥 Bắt đầu tải avatar từ Facebook: {}", imageUrl);

            // Tải ảnh từ URL của Facebook
            URL url = new URL(imageUrl);
            InputStream inputStream = url.openStream();
            byte[] imageBytes = inputStream.readAllBytes();
            inputStream.close();

            log.info("✅ Đã tải {} bytes từ Facebook", imageBytes.length);

            // Tạo tên file unique
            String fileName = "oauth_facebook_" + userId + "_" + System.currentTimeMillis() + ".jpg";

            // Upload lên Firebase Storage
            firebaseStorageService.uploadFile(fileName, imageBytes, "image/jpeg");
            log.info("✅ Đã upload avatar lên Firebase Storage: {}", fileName);

            // Lấy public URL
            String publicUrl = firebaseStorageService.getPublicUrl(fileName);
            log.info("✅ Firebase avatar URL: {}", publicUrl);

            return publicUrl;

        } catch (IOException e) {
            log.error("❌ Lỗi khi tải/upload avatar từ Facebook lên Firebase: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String generateUniqueUsername(String email) {
        String baseUsername = email.contains("@") ? email.split("@")[0] : email;
        String username = baseUsername;
        int counter = 1;

        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}
