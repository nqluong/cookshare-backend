# Backend Implementation Guide - Avatar Upload với Firebase

## 📝 Các file cần tạo/cập nhật:

### 1. DTO Request (✅ Đã tạo)
File: `AvatarUploadUrlRequest.java`
```java
package com.backend.cookshare.authentication.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUploadUrlRequest {
    
    @NotBlank(message = "File name is required")
    private String fileName;
    
    @NotBlank(message = "Content type is required")
    private String contentType;
}
```

### 2. DTO Response (✅ Đã tạo)
File: `AvatarUploadUrlResponse.java`
```java
package com.backend.cookshare.authentication.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUploadUrlResponse {
    
    private String uploadUrl;  // Signed URL for uploading to Firebase
    private String publicUrl;  // Public URL of the uploaded file
}
```

### 3. Cập nhật UserController.java

Thêm import:
```java
import com.backend.cookshare.authentication.dto.request.AvatarUploadUrlRequest;
import com.backend.cookshare.authentication.dto.response.AvatarUploadUrlResponse;
```

Thêm endpoint (trước dấu `}` cuối cùng):
```java
    @PostMapping("/{userId}/avatar/upload-url")
    @PreAuthorize("hasPermission(null, 'USER')")
    public ResponseEntity<AvatarUploadUrlResponse> requestAvatarUploadUrl(
            @PathVariable UUID userId,
            @Valid @RequestBody AvatarUploadUrlRequest request) {
        
        AvatarUploadUrlResponse response = userService.generateAvatarUploadUrl(userId, request);
        return ResponseEntity.ok(response);
    }
```

### 4. Cập nhật UserService interface

Thêm method:
```java
import com.backend.cookshare.authentication.dto.request.AvatarUploadUrlRequest;
import com.backend.cookshare.authentication.dto.response.AvatarUploadUrlResponse;

// ... existing methods ...

AvatarUploadUrlResponse generateAvatarUploadUrl(UUID userId, AvatarUploadUrlRequest request);
```

### 5. Tạo FirebaseStorageService

File: `FirebaseStorageService.java`
```java
package com.backend.cookshare.authentication.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseStorageService {

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    private Storage storage;

    public FirebaseStorageService() throws IOException {
        // Initialize Firebase Storage
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new FileInputStream(credentialsPath)
        );
        
        this.storage = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }

    /**
     * Generate signed URL for uploading file to Firebase Storage
     * @param fileName Name of the file to upload
     * @param contentType MIME type of the file
     * @return Signed URL valid for 15 minutes
     */
    public String generateUploadUrl(String fileName, String contentType) {
        String objectPath = "avatars/" + fileName;
        
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectPath)
                .setContentType(contentType)
                .build();

        // Generate signed URL valid for 15 minutes
        URL signedUrl = storage.signUrl(
                blobInfo,
                15,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withV4Signature()
        );

        return signedUrl.toString();
    }

    /**
     * Get public URL of uploaded file
     * @param fileName Name of the uploaded file
     * @return Public URL
     */
    public String getPublicUrl(String fileName) {
        String objectPath = "avatars/" + fileName;
        return String.format(
            "https://storage.googleapis.com/%s/%s",
            bucketName,
            objectPath
        );
    }
}
```

### 6. Implement trong UserServiceImpl

Thêm dependency injection:
```java
@Autowired
private FirebaseStorageService firebaseStorageService;
```

Implement method:
```java
@Override
public AvatarUploadUrlResponse generateAvatarUploadUrl(UUID userId, AvatarUploadUrlRequest request) {
    // Verify user exists
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // Validate content type (only allow images)
    if (!request.getContentType().startsWith("image/")) {
        throw new IllegalArgumentException("Only image files are allowed");
    }

    // Validate file extension
    String fileName = request.getFileName();
    String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)")) {
        throw new IllegalArgumentException("Invalid file extension. Allowed: jpg, jpeg, png, gif, webp");
    }

    // Generate signed URL for upload
    String uploadUrl = firebaseStorageService.generateUploadUrl(fileName, request.getContentType());
    
    // Get public URL (this will be the avatar URL after upload)
    String publicUrl = firebaseStorageService.getPublicUrl(fileName);

    return AvatarUploadUrlResponse.builder()
            .uploadUrl(uploadUrl)
            .publicUrl(publicUrl)
            .build();
}
```

### 7. Cấu hình application.yml

Thêm config:
```yaml
firebase:
  storage:
    bucket: your-project-id.appspot.com
  credentials:
    path: path/to/your/firebase-credentials.json
```

### 8. Thêm Firebase Admin SDK dependency vào pom.xml

```xml
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>
```

## 🔐 Security Notes:

1. **Authentication**: Endpoint có `@PreAuthorize("hasPermission(null, 'USER')")` - chỉ user đã đăng nhập mới gọi được
2. **Authorization**: Nên thêm check userId trong request phải match với userId đang logged in
3. **File validation**: Kiểm tra content type và extension
4. **Signed URL expiry**: URL chỉ valid 15 phút
5. **Rate limiting**: Cân nhắc thêm rate limiting để tránh abuse

## 📝 Improvement Suggestions:

```java
// Add authorization check
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String currentUsername = auth.getName();
if (!user.getUsername().equals(currentUsername) && !hasAdminRole(auth)) {
    throw new AccessDeniedException("You don't have permission to update this user's avatar");
}
```

## 🧪 Testing:

```bash
# 1. Request upload URL
curl -X POST http://localhost:8080/users/{userId}/avatar/upload-url \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "avatar_123456.jpg",
    "contentType": "image/jpeg"
  }'

# Response:
# {
#   "uploadUrl": "https://storage.googleapis.com/...",
#   "publicUrl": "https://storage.googleapis.com/bucket/avatars/avatar_123456.jpg"
# }

# 2. Upload file using signed URL (from frontend)
curl -X PUT "{uploadUrl}" \
  -H "Content-Type: image/jpeg" \
  --data-binary @avatar.jpg

# 3. Update profile với publicUrl
curl -X PUT http://localhost:8080/users/{userId}/profile \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "avatarUrl": "{publicUrl}"
  }'
```
