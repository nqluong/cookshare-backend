package com.backend.cookshare.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileDownloadService {

    @Value("${file.upload-dir:src/main/resources/static}")
    private String uploadDir;

    /**
     * Tải ảnh từ URL về thư mục static/avatar
     * @param imageUrl URL của ảnh cần tải
     * @param userId ID của user (để tạo tên file unique)
     * @return Đường dẫn tương đối của file đã lưu
     */
    public String downloadImageToAvatar(String imageUrl, UUID userId) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        // Nếu URL đã là local path, giữ nguyên
        if (imageUrl.startsWith("/avatar/") || imageUrl.startsWith("avatar/")) {
            return imageUrl;
        }

        try {
            // Tạo thư mục avatar nếu chưa có
            Path avatarDir = Paths.get(uploadDir, "avatar");
            if (!Files.exists(avatarDir)) {
                Files.createDirectories(avatarDir);
                log.info("Created avatar directory: {}", avatarDir);
            }

            // Lấy extension từ URL
            String extension = getFileExtension(imageUrl);
            if (extension.isEmpty()) {
                extension = ".jpg"; // Default extension
            }

            // Tạo tên file unique
            String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + extension;
            Path targetPath = avatarDir.resolve(fileName);

            // Tải ảnh từ URL
            URL url = new URL(imageUrl);
            try (InputStream in = url.openStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Downloaded avatar from {} to {}", imageUrl, targetPath);
            }

            // Trả về đường dẫn tương đối
            return "/avatar/" + fileName;

        } catch (Exception e) {
            log.error("Failed to download avatar from URL: {}", imageUrl, e);
            // Nếu tải thất bại, trả về URL gốc
            return imageUrl;
        }
    }

    /**
     * Lấy extension từ URL
     */
    private String getFileExtension(String url) {
        try {
            // Loại bỏ query parameters
            String urlWithoutParams = url.split("\\?")[0];

            // Lấy phần sau dấu chấm cuối cùng
            int lastDotIndex = urlWithoutParams.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < urlWithoutParams.length() - 1) {
                String ext = urlWithoutParams.substring(lastDotIndex);
                // Chỉ lấy các extension hợp lệ
                if (ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) {
                    return ext;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract file extension from URL: {}", url);
        }
        return "";
    }

    /**
     * Xóa file avatar cũ nếu có
     */
    public void deleteOldAvatar(String oldAvatarUrl) {
        if (oldAvatarUrl == null || oldAvatarUrl.isEmpty()) {
            return;
        }

        // Chỉ xóa nếu là file local
        if (oldAvatarUrl.startsWith("/avatar/") || oldAvatarUrl.startsWith("avatar/")) {
            try {
                String fileName = oldAvatarUrl.replace("/avatar/", "").replace("avatar/", "");
                Path filePath = Paths.get(uploadDir, "avatar", fileName);

                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Deleted old avatar: {}", filePath);
                }
            } catch (Exception e) {
                log.warn("Failed to delete old avatar: {}", oldAvatarUrl, e);
            }
        }
    }
}

