# 🔥 Hướng dẫn Setup Firebase Storage cho Avatar Upload

## 📋 Bước 1: Tạo Firebase Project

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" hoặc chọn project có sẵn
3. Nhập tên project (ví dụ: `cookshare-project`)
4. Click "Continue" và làm theo hướng dẫn

## 📦 Bước 2: Enable Firebase Storage

1. Trong Firebase Console, chọn project của bạn
2. Vào **Build** > **Storage** từ menu bên trái
3. Click **Get Started**
4. Chọn location (ví dụ: `asia-southeast1`)
5. Click **Done**

## 🔐 Bước 3: Cấu hình Storage Rules

Trong tab **Rules**, thay đổi rules như sau để cho phép upload:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Cho phép đọc avatar công khai
    match /avatars/{fileName} {
      allow read: if true;
      // Chỉ cho phép ghi (upload) nếu đã authenticate
      allow write: if request.auth != null;
    }
  }
}
```

**Lưu ý:** Rules trên chỉ cho phép upload nếu có authentication token. Vì chúng ta dùng signed URL từ backend nên không cần authentication token khi upload.

Nếu muốn sử dụng signed URL (khuyến nghị), dùng rules này:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /avatars/{fileName} {
      allow read: if true;
      allow write: if true; // Signed URL tự động có quyền
    }
  }
}
```

## 🔑 Bước 4: Tạo Service Account Key

1. Trong Firebase Console, click icon ⚙️ > **Project settings**
2. Chọn tab **Service accounts**
3. Click **Generate new private key**
4. Click **Generate key**
5. File JSON sẽ được tải xuống

## 📁 Bước 5: Cấu hình Backend

### 5.1. Copy Firebase Credentials File

Copy file JSON vừa tải xuống vào:
```
cookshare-backend/src/main/resources/firebase-credentials.json
```

**⚠️ QUAN TRỌNG:** Thêm file này vào `.gitignore` để không commit lên Git:

```gitignore
# Firebase credentials
src/main/resources/firebase-credentials.json
```

### 5.2. Cập nhật application.yml

File `application.yml` đã được cập nhật với config:

```yaml
firebase:
  storage:
    bucket: ${FIREBASE_STORAGE_BUCKET:cookshare-project.appspot.com}
  credentials:
    path: ${FIREBASE_CREDENTIALS_PATH:src/main/resources/firebase-credentials.json}
```

**Lấy bucket name:**
- Vào Firebase Console > Storage
- Copy bucket name (có dạng: `cookshare-project.appspot.com`)
- Update trong `application.yml` hoặc environment variable

### 5.3. Environment Variables (Production)

Trong production, nên dùng environment variables:

```bash
export FIREBASE_STORAGE_BUCKET=cookshare-project.appspot.com
export FIREBASE_CREDENTIALS_PATH=/path/to/firebase-credentials.json
```

Hoặc trong Docker:

```yaml
environment:
  - FIREBASE_STORAGE_BUCKET=cookshare-project.appspot.com
  - FIREBASE_CREDENTIALS_PATH=/app/config/firebase-credentials.json
volumes:
  - ./firebase-credentials.json:/app/config/firebase-credentials.json
```

## 🧪 Bước 6: Test Setup

### 6.1. Reload Maven Dependencies

```bash
cd cookshare-backend
./mvnw clean install
```

### 6.2. Khởi động Backend

```bash
./mvnw spring-boot:run
```

### 6.3. Test API với Postman/cURL

```bash
# 1. Login để lấy token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "your-username",
    "password": "your-password"
  }'

# 2. Request upload URL
curl -X POST http://localhost:8080/users/{userId}/avatar/upload-url \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "avatar_1699012345_abc123.jpg",
    "contentType": "image/jpeg"
  }'

# Response sẽ có dạng:
{
  "uploadUrl": "https://storage.googleapis.com/cookshare-project.appspot.com/avatars/avatar_1699012345_abc123.jpg?...",
  "publicUrl": "https://storage.googleapis.com/cookshare-project.appspot.com/avatars/avatar_1699012345_abc123.jpg"
}

# 3. Upload file (từ frontend hoặc test với curl)
curl -X PUT "{uploadUrl}" \
  -H "Content-Type: image/jpeg" \
  --data-binary @/path/to/your/avatar.jpg
```

## 🛡️ Security Best Practices

### 1. Bảo vệ Service Account Key
- ✅ Thêm vào `.gitignore`
- ✅ Không commit lên Git
- ✅ Sử dụng environment variables trong production
- ✅ Rotate key định kỳ

### 2. CORS Configuration
Nếu frontend upload trực tiếp, cần cấu hình CORS trong Firebase Storage:

```json
[
  {
    "origin": ["http://localhost:19006", "https://your-domain.com"],
    "method": ["GET", "PUT"],
    "maxAgeSeconds": 3600
  }
]
```

Cấu hình bằng gsutil:

```bash
gsutil cors set cors.json gs://cookshare-project.appspot.com
```

### 3. Rate Limiting
Xem xét thêm rate limiting cho endpoint upload URL:

```java
// Trong UserController
@RateLimiter(name = "avatarUpload", fallbackMethod = "uploadRateLimitFallback")
@PostMapping("/{userId}/avatar/upload-url")
```

## 📝 Troubleshooting

### Lỗi: "The import com.google cannot be resolved"
- Maven dependencies chưa được tải xuống
- Chạy: `./mvnw clean install`
- Reload project trong IDE

### Lỗi: "FileNotFoundException: firebase-credentials.json"
- Kiểm tra file có tồn tại tại đúng path không
- Kiểm tra `application.yml` có đúng path không

### Lỗi: "Access Denied" khi upload
- Kiểm tra Storage Rules
- Kiểm tra signed URL có còn hiệu lực không (15 phút)

### Lỗi: "Invalid bucket name"
- Kiểm tra bucket name trong `application.yml`
- Lấy đúng bucket name từ Firebase Console

## ✅ Checklist

- [ ] Tạo Firebase project
- [ ] Enable Firebase Storage
- [ ] Cấu hình Storage Rules
- [ ] Tạo và tải Service Account Key
- [ ] Copy `firebase-credentials.json` vào `src/main/resources/`
- [ ] Thêm `firebase-credentials.json` vào `.gitignore`
- [ ] Cập nhật bucket name trong `application.yml`
- [ ] Chạy `./mvnw clean install`
- [ ] Test API với Postman/cURL
- [ ] Test upload từ frontend

## 📚 References

- [Firebase Storage Documentation](https://firebase.google.com/docs/storage)
- [Firebase Admin SDK Setup](https://firebase.google.com/docs/admin/setup)
- [Google Cloud Storage Signed URLs](https://cloud.google.com/storage/docs/access-control/signed-urls)
