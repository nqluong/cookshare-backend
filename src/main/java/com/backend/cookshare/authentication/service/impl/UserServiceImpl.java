package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.request.AvatarUploadUrlRequest;
import com.backend.cookshare.authentication.dto.request.UpdateUserProfileRequest;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.dto.response.AvatarUploadUrlResponse;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FirebaseStorageService firebaseStorageService;

    /**
     * Lấy thông tin user đang đăng nhập từ SecurityContext
     */
    private User getCurrentUser() {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public String createUser(UserRequest user) {

        User saveUser = User.builder()
                .username(user.getUsername())
                .passwordHash(passwordEncoder.encode(user.getPassword()))
                .email(user.getEmail())
                .fullName(user.getFullname())
                .createdAt(LocalDateTime.now())
                .lastActive(LocalDateTime.now())
                .build();
        userRepository.save(saveUser);
        return "Ban da dang ky thanh cong!";
    }

    @Override
    public Optional<User> getUserById(UUID userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> getUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(UUID userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void updateLastActive(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActive(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Override
    public void updateUserToken(String token, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRefreshToken(token);
        this.userRepository.save(user);
    }

    @Override
    public User getUserByRefreshTokenAndUsername(String token, String username) {
        return this.userRepository.findByRefreshTokenAndUsername(token, username);
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        // Tìm user theo username
        User user = getUserByUsernameOrEmail(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra mật khẩu hiện tại có đúng không
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // Kiểm tra mật khẩu mới có trùng với mật khẩu hiện tại không
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new CustomException(ErrorCode.SAME_PASSWORD);
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public User updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        // Lấy thông tin user đang đăng nhập
        User currentUser = getCurrentUser();

        // Kiểm tra xem userId truyền vào có phải của user đang đăng nhập không
        if (!currentUser.getUserId().equals(userId)) {
            log.error("❌ User {} đang cố gắng cập nhật profile của user {}",
                    currentUser.getUserId(), userId);
            throw new CustomException(ErrorCode.UNAUTHORIZED_UPDATE);
        }

        // Tìm user theo ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra nếu username mới đã tồn tại (và không phải của chính user này)
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new CustomException(ErrorCode.USERNAME_EXISTED);
            }
            user.setUsername(request.getUsername());
        }

        // Kiểm tra nếu email mới đã tồn tại (và không phải của chính user này)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new CustomException(ErrorCode.EMAIL_EXISTED);
            }
            user.setEmail(request.getEmail());
            // Nếu đổi email mới thì cần verify lại
            user.setEmailVerified(false);
        }

        // Cập nhật các trường khác nếu có
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getAvatarUrl() != null) {
            // Xóa avatar cũ trước khi cập nhật avatar mới
            String oldAvatarUrl = user.getAvatarUrl();
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()
                    && !oldAvatarUrl.equals(request.getAvatarUrl())) {
                log.info("🗑️ Xóa avatar cũ trước khi cập nhật: {}", oldAvatarUrl);
                firebaseStorageService.deleteAvatar(oldAvatarUrl);
            }

            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        // Cập nhật thời gian
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Override
    public AvatarUploadUrlResponse generateAvatarUploadUrl(UUID userId, AvatarUploadUrlRequest request) {
        log.info("🔐 Tạo upload URL cho avatar của user: {}", userId);

        // Kiểm tra Firebase Storage đã được khởi tạo chưa
        if (!firebaseStorageService.isInitialized()) {
            log.error("❌ Firebase Storage chưa được khởi tạo");
            throw new IllegalStateException("Firebase Storage chưa được cấu hình. Vui lòng liên hệ admin.");
        }

        // Kiểm tra user có tồn tại không
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ Không tìm thấy user với ID: {}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        // Validate content type (chỉ cho phép ảnh)
        if (!request.getContentType().startsWith("image/")) {
            log.error("❌ Content type không hợp lệ: {}", request.getContentType());
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh");
        }

        // Validate phần mở rộng file
        String fileName = request.getFileName();
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            log.error("❌ Phần mở rộng file không hợp lệ: {}", extension);
            throw new IllegalArgumentException("Phần mở rộng file không hợp lệ. Chấp nhận: jpg, jpeg, png, gif, webp");
        }

        log.info("📝 Tạo signed URL cho file: {}, content type: {}", fileName, request.getContentType());

        // Tạo signed URL để upload
        String uploadUrl = firebaseStorageService.generateAvatarUploadUrl(fileName, request.getContentType());

        // Lấy public URL (đây sẽ là URL avatar sau khi upload)
        String publicUrl = firebaseStorageService.getAvatarPublicUrl(fileName);

        log.info("✅ Tạo upload URL thành công cho user: {}", userId);
        log.info("📤 Upload URL: {}", uploadUrl);
        log.info("🌐 Public URL: {}", publicUrl);

        return AvatarUploadUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .publicUrl(publicUrl)
                .build();
    }

    // ==================== ADMIN METHODS ====================

    @Override
    public Page<User> getAllUsersWithPagination(String search, Pageable pageable) {
        log.info("Admin fetching users with search: {}, page: {}, size: {}",
                search, pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAllWithSearch(search, pageable);
    }

    @Override
    public User getUserDetailById(UUID userId) {
        log.info("Admin fetching user details for userId: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public void banUser(UUID userId, String reason) {
        log.info("Admin banning user: {} with reason: {}", userId, reason);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new CustomException(ErrorCode.USER_ALREADY_BANNED);
        }

        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} has been banned successfully", userId);
    }

    @Override
    public void unbanUser(UUID userId) {
        log.info("Admin unbanning user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new CustomException(ErrorCode.USER_ALREADY_ACTIVE);
        }

        user.setIsActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} has been unbanned successfully", userId);
    }

    @Override
    public void deleteUserByAdmin(UUID userId) {
        log.info("Admin deleting user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // You might want to add additional checks here, like preventing deletion of
        // other admins
        // or performing soft delete instead of hard delete
        userRepository.delete(user);

        log.info("User {} has been deleted successfully", userId);
    }
}