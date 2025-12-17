# CookShare Backend

Backend API cho ứng dụng chia sẻ công thức nấu ăn CookShare, được xây dựng với Spring Boot và PostgreSQL.

## Mục lục

- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Bắt đầu nhanh (5 phút)](#-bắt-đầu-nhanh-5-phút)
- [Cài đặt chi tiết](#cài-đặt-chi-tiết)
- [Quản lý Docker Compose](#quản-lý-docker-compose)


## Yêu cầu hệ thống

- **Java 21** hoặc cao hơn
- **Maven 3.9+**
- **Docker** và **Docker Compose** (cho deployment)
- **PostgreSQL 15+** (nếu chạy local không dùng Docker)

## Công nghệ sử dụng

- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL 15
- **Authentication**: JWT, OAuth2 (Google, Facebook)
- **Storage**: Firebase Storage
- **Email**: Gmail SMTP
- **WebSocket**: Real-time notifications

## 🚀 Bắt đầu nhanh

### Bước 1: Chuẩn bị file môi trường

```bash
# Copy file mẫu
cp .env.example .env
```

Chỉnh sửa file `.env` với thông tin của bạn:

```env
# Database
POSTGRES_PASSWORD=your_secure_password

# JWT Secret (generate: openssl rand -base64 32)
JWT_SECRET=your_jwt_secret_here

# Email (Gmail App Password)
USERNAME_MAIL=your-email@gmail.com
PASSWORD_MAIL=your_gmail_app_password

# Firebase
FIREBASE_STORAGE_BUCKET=your-project-id.appspot.com

# OAuth2 (optional - có thể bỏ qua nếu không dùng)
GG_CLIENT_ID=your-google-client-id
GG_CLIENT_SECRET=your-google-client-secret
FB_CLIENT_ID=your-facebook-app-id
FB_CLIENT_SECRET=your-facebook-app-secret
```

**Lưu ý quan trọng:**

1. **JWT Secret**: Phải là chuỗi base64, tối thiểu 256 bits
   ```bash
   # Generate trên Linux/Mac
   openssl rand -base64 32
   
   # Generate trên Windows (PowerShell)
   [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
   ```

2. **Gmail App Password**: 
   - Không dùng password thường
   - Tạo App Password tại: https://myaccount.google.com/apppasswords
   - Hướng dẫn: https://support.google.com/accounts/answer/185833

3. **OAuth2 Credentials**:
   - Google: https://console.cloud.google.com/
   - Facebook: https://developers.facebook.com/

### Bước 2: Chuẩn bị Firebase credentials

Tạo file `firebase-credentials.json` trong thư mục gốc:

```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "your-private-key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\nYour-Private-Key-Here\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-xxxxx@your-project-id.iam.gserviceaccount.com",
  "client_id": "your-client-id",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-xxxxx%40your-project-id.iam.gserviceaccount.com"
}
```

**Lấy Firebase credentials:**
1. Vào Firebase Console: https://console.firebase.google.com/
2. Chọn project của bạn
3. Project Settings > Service Accounts
4. Click "Generate new private key"
5. Lưu file JSON và đổi tên thành `firebase-credentials.json`

Hoặc sử dụng file mẫu `firebase-credentials.example.json` và cập nhật thông tin.

### Bước 3: Khởi động services

```bash
# Start tất cả services (backend + database)
docker-compose up -d

# Xem logs
docker-compose logs -f

# Hoặc xem logs từng service
docker-compose logs -f backend
docker-compose logs -f postgres
```

### Bước 4: Import database

Database sẽ **tự động import** khi khởi động lần đầu tiên từ file `Cloud_SQL_Export_2025-12-16 (08_49_10).sql`.

**Lưu ý**: Import tự động chỉ chạy khi tạo database lần đầu. Nếu cần import lại:

```bash
# Xóa volume và start lại
docker-compose down -v
docker-compose up -d

# Hoặc import thủ công
docker exec -i cookshare-postgres psql -U cookshare_user -d cookshare_db < "Cloud_SQL_Export_2025-12-16 (08_49_10).sql"
```

Xem chi tiết trong file [IMPORT_DATABASE.md](IMPORT_DATABASE.md)

### Bước 5: Kiểm tra

- **Backend API**: http://localhost:8080
- **Database**: localhost:5432
- **pgAdmin** (nếu chạy với --profile tools): http://localhost:5050

✅ **Xong! Backend đã sẵn sàng.**

---

## Cài đặt chi tiết

### Cấu trúc dự án

```
cookshare/
├── src/
│   ├── main/
│   │   ├── java/com/backend/cookshare/
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── images/                           # Local image storage
├── logs/                             # Application logs
├── firebase-credentials.json         # Firebase service account
├── .env                              # Environment variables (gitignored)
├── .env.example                      # Environment template
├── docker-compose.yml                # Docker Compose configuration
├── Dockerfile                        # Docker build file
├── pom.xml                           # Maven configuration
└── README.md                         # This file
```

### Option 1: Chạy với Docker Compose (Khuyến nghị)

Docker Compose sẽ tự động:
- Khởi động PostgreSQL database
- Build và chạy Spring Boot backend
- Cấu hình network giữa các services
- Mount volumes cho logs và images

```bash
# Start services
docker-compose up -d

# Xem status
docker-compose ps

# Stop services
docker-compose down

# Restart services
docker-compose restart

# Rebuild và restart
docker-compose build --no-cache
docker-compose up -d
```

### Option 2: Chạy local (Development)

#### 1. Cài đặt PostgreSQL

```bash
# Chạy PostgreSQL với Docker
docker run -d \
  --name cookshare-postgres \
  -e POSTGRES_DB=cookshare_db \
  -e POSTGRES_USER=cookshare_user \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  postgres:15-alpine
```

#### 2. Cấu hình environment variables

Tạo file `local.env` hoặc export biến môi trường:

```bash
export HOST=localhost
export PORT=5432
export DATABASE=cookshare_db
export USERNAME_DB=cookshare_user
export PASSWORD_DB=your_password
export USERNAME_ADMIN=admin
export PASSWORD_ADMIN=admin123
export JWT_SECRET=your_jwt_secret
export USERNAME_MAIL=your-email@gmail.com
export PASSWORD_MAIL=your_app_password
export GG_CLIENT_ID=your-google-client-id
export GG_CLIENT_SECRET=your-google-client-secret
export GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
export FB_CLIENT_ID=your-facebook-app-id
export FB_CLIENT_SECRET=your-facebook-app-secret
export FACEBOOK_REDIRECT_URI=http://localhost:8080/login/oauth2/code/facebook
export FIREBASE_STORAGE_BUCKET=your-project.appspot.com
export FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json
```

#### 3. Build và chạy

```bash
# Build project
mvn clean package -DskipTests

# Chạy application
java -jar target/cookshare-*.jar

# Hoặc chạy trực tiếp với Maven
mvn spring-boot:run
```

