package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.google.cloud.storage.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FirebaseStorageServiceImpl implements FirebaseStorageService {
    private static final String AVATAR_FOLDER = "avatars";
    private static final String RECIPE_IMAGE_FOLDER = "recipe_images";
    private static final int SIGNED_URL_DURATION_MINUTES = 15;

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    private Storage storage;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(Paths.get(credentialsPath))) {
                log.warn("Firebase credentials file không tồn tại tại: {}", credentialsPath);
                log.warn("Firebase Storage service sẽ không hoạt động");
                return;
            }

            log.info("Đang khởi tạo Firebase Storage...");

            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream(credentialsPath));

            this.storage = StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();

            this.initialized = true;
            log.info("Firebase Storage đã được khởi tạo thành công!");
            log.info("Bucket: {}", bucketName);

        } catch (IOException e) {
            log.error("Lỗi khi khởi tạo Firebase Storage: {}", e.getMessage());
            log.warn("Firebase Storage service sẽ không hoạt động. Vui lòng kiểm tra credentials.");
        }
    }

    @Override
    public String generateAvatarUploadUrl(String fileName, String contentType) {
        return generateUploadUrl(AVATAR_FOLDER, fileName, contentType);
    }

    @Override
    public String getAvatarPublicUrl(String fileName) {
        return getPublicUrl(AVATAR_FOLDER, fileName);
    }

    @Override
    public void uploadAvatar(String fileName, byte[] fileBytes, String contentType) {
        uploadFile(AVATAR_FOLDER, fileName, fileBytes, contentType);
    }

    @Override
    public boolean deleteAvatar(String avatarUrl) {
        return deleteFile(avatarUrl, AVATAR_FOLDER);
    }

    @Override
    public String generateRecipeImageUploadUrl(String fileName, String contentType) {
        return generateUploadUrl(RECIPE_IMAGE_FOLDER, fileName, contentType);
    }

    @Override
    public String uploadRecipeImage(String fileName, byte[] fileBytes, String contentType) {
        uploadFile(RECIPE_IMAGE_FOLDER, fileName, fileBytes, contentType);
        return getPublicUrl(RECIPE_IMAGE_FOLDER, fileName);
    }

    @Override
    public boolean deleteRecipeImage(String imageUrl) {
        return deleteFile(imageUrl, RECIPE_IMAGE_FOLDER);
    }

    @Override
    public String convertPathToFirebaseUrl(String localPath) {
        if (localPath == null || localPath.isEmpty()) {
            return null;
        }

        if (localPath.startsWith("http://") || localPath.startsWith("https://")) {
            return localPath;
        }

        String normalizedPath = localPath.replace("\\", "/");

        String encodedPath = normalizedPath.replace("/", "%2F");

        return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                bucketName,
                encodedPath
        );
    }

    @Override
    public List<String> convertPathsToFirebaseUrls(List<String> localPaths) {
        if (localPaths == null || localPaths.isEmpty()) {
            return Collections.emptyList();
        }

        return localPaths.stream()
                .map(this::convertPathToFirebaseUrl)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Tạo signed URL để upload file
     */
    private String generateUploadUrl(String folder, String fileName, String contentType) {
        validateInitialized();

        String objectPath = folder + "/" + fileName;

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectPath)
                .setContentType(contentType)
                .build();

        URL signedUrl = storage.signUrl(
                blobInfo,
                SIGNED_URL_DURATION_MINUTES,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withV4Signature());

        log.info("Generated upload URL for: {}", objectPath);
        return signedUrl.toString();
    }

    /**
     * Lấy public URL của file
     */
    private String getPublicUrl(String folder, String fileName) {
        String objectPath = folder + "/" + fileName;
        String encodedPath = objectPath.replace("/", "%2F");

        return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                bucketName,
                encodedPath);
    }

    /**
     * Upload file trực tiếp lên Firebase Storage
     */
    private void uploadFile(String folder, String fileName, byte[] fileBytes, String contentType) {
        validateInitialized();

        String objectPath = folder + "/" + fileName;

        BlobId blobId = BlobId.of(bucketName, objectPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        storage.create(blobInfo, fileBytes);
        log.info("Đã upload file lên Firebase Storage: {}", objectPath);
    }

    /**
     * Xóa file khỏi Firebase Storage
     */
    private boolean deleteFile(String fileUrl, String folder) {
        if (!initialized) {
            log.warn("Firebase Storage chưa được khởi tạo, không thể xóa file");
            return false;
        }

        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        try {
            // Chỉ xóa nếu là Firebase Storage URL
            if (!fileUrl.contains("firebasestorage.googleapis.com")) {
                log.info("URL không phải từ Firebase Storage, bỏ qua xóa: {}", fileUrl);
                return false;
            }

            String fileName = extractFileNameFromUrl(fileUrl, folder);
            if (fileName == null) {
                log.warn("Không thể extract filename từ URL: {}", fileUrl);
                return false;
            }

            String objectPath = folder + "/" + fileName;
            BlobId blobId = BlobId.of(bucketName, objectPath);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                log.info("Đã xóa file khỏi Firebase Storage: {}", objectPath);
            } else {
                log.warn("Không tìm thấy file để xóa: {}", objectPath);
            }

            return deleted;

        } catch (Exception e) {
            log.error("Lỗi khi xóa file từ Firebase Storage: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract filename từ Firebase Storage URL
     */
    private String extractFileNameFromUrl(String url, String folder) {
        try {
            String pattern = folder + "%2F";
            int startIndex = url.indexOf(pattern);

            if (startIndex == -1) {
                pattern = folder + "/";
                startIndex = url.indexOf(pattern);
            }

            if (startIndex == -1) {
                return null;
            }

            startIndex += pattern.length();

            int endIndex = url.indexOf('?', startIndex);
            if (endIndex == -1) {
                endIndex = url.indexOf('&', startIndex);
            }
            if (endIndex == -1) {
                endIndex = url.length();
            }

            String fileName = url.substring(startIndex, endIndex);
            return URLDecoder.decode(fileName, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Lỗi khi extract filename từ URL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate Firebase Storage đã được khởi tạo
     */
    private void validateInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "Firebase Storage chưa được khởi tạo. Vui lòng kiểm tra firebase-credentials.json");
        }
    }
}
