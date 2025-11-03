package com.backend.cookshare.authentication.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FirebaseStorageService {

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    private Storage storage;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        try {
            // Kiểm tra file credentials có tồn tại không
            if (!Files.exists(Paths.get(credentialsPath))) {
                log.warn("⚠️ Firebase credentials file không tồn tại tại: {}", credentialsPath);
                log.warn("⚠️ Firebase Storage service sẽ không hoạt động");
                return;
            }

            log.info("🔧 Đang khởi tạo Firebase Storage...");

            // Khởi tạo Firebase Storage
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream(credentialsPath));

            this.storage = StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();

            this.initialized = true;
            log.info("✅ Firebase Storage đã được khởi tạo thành công!");
            log.info("📦 Bucket: {}", bucketName);

        } catch (IOException e) {
            log.error("❌ Lỗi khi khởi tạo Firebase Storage: {}", e.getMessage());
            log.warn("⚠️ Firebase Storage service sẽ không hoạt động. Vui lòng kiểm tra credentials.");
        }
    }

    /**
     * Tạo signed URL để upload file lên Firebase Storage
     * 
     * @param fileName    Tên file cần upload
     * @param contentType MIME type của file
     * @return Signed URL có hiệu lực trong 15 phút
     */
    public String generateUploadUrl(String fileName, String contentType) {
        if (!initialized) {
            throw new IllegalStateException(
                    "Firebase Storage chưa được khởi tạo. Vui lòng kiểm tra firebase-credentials.json");
        }

        String objectPath = "avatars/" + fileName;

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectPath)
                .setContentType(contentType)
                .build();

        // Tạo signed URL có hiệu lực trong 15 phút
        URL signedUrl = storage.signUrl(
                blobInfo,
                15,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withV4Signature());

        return signedUrl.toString();
    }

    /**
     * Lấy public URL của file đã upload
     * 
     * @param fileName Tên file đã upload
     * @return Public URL
     */
    public String getPublicUrl(String fileName) {
        String objectPath = "avatars/" + fileName;
        // Encode objectPath để xử lý ký tự đặc biệt
        String encodedPath = objectPath.replace("/", "%2F");

        // Format:
        // https://firebasestorage.googleapis.com/v0/b/[bucket]/o/[path]?alt=media
        return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                bucketName,
                encodedPath);
    }

    /**
     * Upload file trực tiếp lên Firebase Storage (dùng cho OAuth avatar)
     * 
     * @param fileName    Tên file
     * @param fileBytes   Nội dung file dạng byte array
     * @param contentType MIME type của file
     */
    public void uploadFile(String fileName, byte[] fileBytes, String contentType) {
        if (!initialized) {
            throw new IllegalStateException(
                    "Firebase Storage chưa được khởi tạo. Vui lòng kiểm tra firebase-credentials.json");
        }

        String objectPath = "avatars/" + fileName;

        BlobId blobId = BlobId.of(bucketName, objectPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        // Upload file lên Firebase Storage
        storage.create(blobInfo, fileBytes);
        log.info("✅ Đã upload file lên Firebase Storage: {}", objectPath);
    }

    /**
     * Xóa file khỏi Firebase Storage (dùng khi user thay đổi avatar)
     * 
     * @param avatarUrl URL đầy đủ của avatar cần xóa
     * @return true nếu xóa thành công, false nếu không thể xóa
     */
    public boolean deleteAvatarByUrl(String avatarUrl) {
        if (!initialized) {
            log.warn("⚠️ Firebase Storage chưa được khởi tạo, không thể xóa avatar");
            return false;
        }

        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return false;
        }

        try {
            // Chỉ xóa nếu là Firebase Storage URL
            if (!avatarUrl.contains("firebasestorage.googleapis.com")) {
                log.info("ℹ️ Avatar URL không phải từ Firebase Storage, bỏ qua xóa: {}", avatarUrl);
                return false;
            }

            // Extract filename từ URL
            // Format:
            // https://firebasestorage.googleapis.com/v0/b/[bucket]/o/avatars%2F[filename]?alt=media
            String fileName = extractFileNameFromUrl(avatarUrl);
            if (fileName == null) {
                log.warn("⚠️ Không thể extract filename từ URL: {}", avatarUrl);
                return false;
            }

            // Xóa file
            String objectPath = "avatars/" + fileName;
            BlobId blobId = BlobId.of(bucketName, objectPath);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                log.info("✅ Đã xóa avatar cũ khỏi Firebase Storage: {}", objectPath);
            } else {
                log.warn("⚠️ Không tìm thấy file để xóa: {}", objectPath);
            }

            return deleted;

        } catch (Exception e) {
            log.error("❌ Lỗi khi xóa avatar từ Firebase Storage: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract filename từ Firebase Storage URL
     */
    private String extractFileNameFromUrl(String url) {
        try {
            // URL format:
            // https://firebasestorage.googleapis.com/v0/b/[bucket]/o/avatars%2F[filename]?alt=media
            // Tìm phần sau "avatars%2F" hoặc "avatars/"
            String pattern = "avatars%2F";
            int startIndex = url.indexOf(pattern);

            if (startIndex == -1) {
                pattern = "avatars/";
                startIndex = url.indexOf(pattern);
            }

            if (startIndex == -1) {
                return null;
            }

            startIndex += pattern.length();

            // Tìm dấu ? hoặc & (query parameters)
            int endIndex = url.indexOf('?', startIndex);
            if (endIndex == -1) {
                endIndex = url.indexOf('&', startIndex);
            }
            if (endIndex == -1) {
                endIndex = url.length();
            }

            String fileName = url.substring(startIndex, endIndex);

            // Decode URL encoding nếu có
            fileName = java.net.URLDecoder.decode(fileName, "UTF-8");

            return fileName;

        } catch (Exception e) {
            log.error("❌ Lỗi khi extract filename từ URL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Kiểm tra Firebase Storage đã được khởi tạo chưa
     */
    public boolean isInitialized() {
        return initialized;
    }
}
