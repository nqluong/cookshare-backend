package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.recipe_management.service.FileStorageService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class FirebaseStorageServiceImpl implements FileStorageService {

    private static final String BUCKET_NAME = "cookshare-app-33834.appspot.com";
    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                if (credentialsPath == null || credentialsPath.startsWith("${")) {
                    log.error("Firebase credentials path not set. Did you set the FIREBASE_CREDENTIALS_PATH environment variable?");
                    return;
                }

                var serviceAccount = new FileInputStream(credentialsPath);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setStorageBucket(BUCKET_NAME)
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            }
        } catch (Exception e) {
            log.error("Firebase initialization error: {}", e.getMessage(), e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File trống, không thể upload");
        }

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Bucket bucket = StorageClient.getInstance().bucket();
            Blob blob = bucket.create(fileName, file.getBytes(), file.getContentType());
            blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

            return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileName);
        } catch (IOException e) {
            log.error("Lỗi khi upload file lên Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể upload file lên Firebase", e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isBlank()) return;

            String fileName = extractFileName(fileUrl);
            if (fileName == null) return;

            Storage storage = StorageOptions.getDefaultInstance().getService();
            boolean deleted = storage.delete(BUCKET_NAME, fileName);

            if (deleted) {
                log.info("Đã xóa file trên Firebase: {}", fileName);
            } else {
                log.warn("Không tìm thấy file cần xóa: {}", fileName);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa file Firebase: {}", e.getMessage(), e);
        }
    }

    private String extractFileName(String fileUrl) {
        try {
            if (fileUrl.contains(BUCKET_NAME)) {
                return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            log.error("Không thể tách tên file từ URL: {}", fileUrl);
        }
        return null;
    }
}
