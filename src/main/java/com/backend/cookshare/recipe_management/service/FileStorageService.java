package com.backend.cookshare.recipe_management.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Upload file lên Firebase Storage và trả về URL public
     */
    String uploadFile(MultipartFile file);

    /**
     * Xóa file khỏi Firebase Storage bằng tên file hoặc URL
     */
    void deleteFile(String fileUrl);
}
