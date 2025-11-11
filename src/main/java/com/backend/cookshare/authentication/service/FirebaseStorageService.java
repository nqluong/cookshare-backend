package com.backend.cookshare.authentication.service;


import java.util.List;

public interface FirebaseStorageService {

    /**
     * Tạo signed URL để upload avatar lên Firebase Storage
     * @param fileName Tên file cần upload
     * @param contentType MIME type của file
     * @return Signed URL có hiệu lực trong 15 phút
     */
    String generateAvatarUploadUrl(String fileName, String contentType);

    /**
     * Lấy public URL của avatar đã upload
     * @param fileName Tên file đã upload
     * @return Public URL
     */
    String getAvatarPublicUrl(String fileName);

    /**
     * Upload avatar trực tiếp lên Firebase Storage (dùng cho OAuth)
     * @param fileName Tên file
     * @param fileBytes Nội dung file dạng byte array
     * @param contentType MIME type của file
     */
    void uploadAvatar(String fileName, byte[] fileBytes, String contentType);

    /**
     * Xóa avatar khỏi Firebase Storage
     * @param avatarUrl URL đầy đủ của avatar cần xóa
     * @return true nếu xóa thành công, false nếu không thể xóa
     */
    boolean deleteAvatar(String avatarUrl);

    /**
     * Tạo signed URL để upload recipe image
     * @param fileName Tên file cần upload
     * @param contentType MIME type của file
     * @return Signed URL có hiệu lực trong 15 phút
     */
    String generateRecipeImageUploadUrl(String fileName, String contentType);

    /**
     * Upload recipe image trực tiếp lên Firebase Storage
     * @param fileName Tên file
     * @param fileBytes Nội dung file dạng byte array
     * @param contentType MIME type của file
     * @return Public URL của image đã upload
     */
    String uploadRecipeImage(String fileName, byte[] fileBytes, String contentType);

    /**
     * Xóa recipe image khỏi Firebase Storage
     * @param imageUrl URL đầy đủ của image cần xóa
     * @return true nếu xóa thành công, false nếu không thể xóa
     */
    boolean deleteRecipeImage(String imageUrl);

    /**
     * Convert local path thành Firebase public URL
     * @param localPath Path local (vd: "recipe_images\com-chien-ca-man.jpg")
     * @return Firebase public URL
     */
    String convertPathToFirebaseUrl(String localPath);

    /**
     * Convert danh sách local paths thành Firebase URLs
     * @param localPaths Danh sách local paths
     * @return Danh sách Firebase URLs
     */
    List<String> convertPathsToFirebaseUrls(List<String> localPaths);

    /**
     * Kiểm tra Firebase Storage đã được khởi tạo chưa
     * @return true nếu đã khởi tạo thành công
     */
    boolean isInitialized();
}
