# Hướng dẫn cấu hình Firebase

Hướng dẫn chi tiết để lấy Firebase credentials cho CookShare Backend.

## Bước 1: Tạo Firebase Project

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** hoặc **"Create a project"**
3. Nhập tên project (ví dụ: `cookshare-app`)
4. (Optional) Enable Google Analytics
5. Click **"Create project"**

## Bước 2: Enable Firebase Storage

1. Trong Firebase Console, chọn project của bạn
2. Vào menu bên trái, chọn **"Build"** > **"Storage"**
3. Click **"Get started"**
4. Chọn location (ví dụ: `asia-southeast1` cho Việt Nam)
5. Click **"Done"**

## Bước 3: Cấu hình Storage Rules (Optional)

Trong tab **"Rules"**, cập nhật rules cho phép upload:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      // Allow authenticated users to upload
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

## Bước 4: Lấy Service Account Credentials

### Cách 1: Từ Firebase Console (Khuyến nghị)

1. Trong Firebase Console, click vào **⚙️ (Settings icon)** > **"Project settings"**
2. Chọn tab **"Service accounts"**
3. Scroll xuống phần **"Firebase Admin SDK"**
4. Chọn **"Java"** (hoặc bất kỳ ngôn ngữ nào)
5. Click **"Generate new private key"**
6. Confirm và download file JSON
7. Đổi tên file thành `firebase-credentials.json`
8. Copy file vào thư mục gốc của project

### Cách 2: Từ Google Cloud Console

1. Truy cập [Google Cloud Console](https://console.cloud.google.com/)
2. Chọn project Firebase của bạn
3. Vào **"IAM & Admin"** > **"Service Accounts"**
4. Tìm service account có email dạng: `firebase-adminsdk-xxxxx@your-project.iam.gserviceaccount.com`
5. Click vào service account đó
6. Chọn tab **"Keys"**
7. Click **"Add Key"** > **"Create new key"**
8. Chọn **"JSON"** và click **"Create"**
9. File JSON sẽ được download tự động

## Bước 5: Lấy Storage Bucket Name

1. Trong Firebase Console > **"Project settings"**
2. Scroll xuống phần **"Your apps"**
3. Hoặc vào **"Storage"** > **"Files"**
4. Bucket name có dạng: `your-project-id.appspot.com`

## Bước 6: Cấu hình trong Project

### 1. Đặt file credentials

```bash
# Copy file vào thư mục gốc
cp ~/Downloads/your-project-xxxxx.json ./firebase-credentials.json
```

### 2. Cập nhật file .env

```env
FIREBASE_STORAGE_BUCKET=your-project-id.appspot.com
```

### 3. Kiểm tra file credentials

File `firebase-credentials.json` phải có cấu trúc như sau:

```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "abc123...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-xxxxx@your-project-id.iam.gserviceaccount.com",
  "client_id": "123456789...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/...",
  "universe_domain": "googleapis.com"
}
```

## Bước 7: Test kết nối

```bash
# Start backend
docker-compose up -d

# Xem logs để kiểm tra Firebase đã kết nối thành công
docker-compose logs -f backend | grep -i firebase
```

Nếu thành công, bạn sẽ thấy log tương tự:
```
Firebase initialized successfully
```
