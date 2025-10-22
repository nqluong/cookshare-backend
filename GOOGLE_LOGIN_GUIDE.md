# Hướng Dẫn Sử Dụng Đăng Nhập Google

## Tổng Quan
Chức năng đăng nhập Google đã được tích hợp vào hệ thống CookShare, cho phép người dùng đăng nhập nhanh chóng bằng tài khoản Google của họ.

## Các File Đã Tạo

### 1. DTO (Data Transfer Objects)
- **GoogleUserInfo.java**: Chứa thông tin user từ Google
- **GoogleTokenResponse.java**: Chứa response token từ Google OAuth

### 2. Service
- **GoogleOAuthService.java**: Xử lý logic đăng nhập Google
  - Lấy access token từ Google
  - Lấy thông tin user từ Google
  - Tìm hoặc tạo user trong database
  - Tạo JWT tokens cho ứng dụng

### 3. Controller
- **GoogleAuthController.java**: Xử lý các API endpoints cho Google OAuth

### 4. Configuration
- **RestTemplateConfig.java**: Cấu hình RestTemplate để gọi API Google
- **SecurityConfiguration.java**: Đã được cập nhật để cho phép truy cập các endpoint Google OAuth

## API Endpoints

### 1. Khởi động quá trình đăng nhập
```
GET /auth/google/login
```
- Endpoint này sẽ redirect người dùng đến trang đăng nhập Google
- Sau khi đăng nhập thành công, Google sẽ redirect về callback URL

### 2. Callback từ Google
```
GET /auth/google/callback?code={authorization_code}
```
- Google sẽ gọi endpoint này sau khi user đăng nhập thành công
- Trả về JWT tokens (access_token, refresh_token) và thông tin user

### 3. Xác thực với authorization code (cho mobile/frontend)
```
POST /auth/google/authenticate?code={authorization_code}
```
- Endpoint này dành cho mobile app hoặc SPA
- Gửi authorization code để nhận JWT tokens

## Cấu Hình

Trong file `application.yml`, các thông tin sau đã được cấu hình:

```yaml
spring:
  security:
    oauth2:
      registration:
        google:
          client-id: 930367083527-chtbv453jhlmdllanvtrf18sfigeqsvj.apps.googleusercontent.com
          client-secret: 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
          redirect-uri: http://localhost:8080/auth/google/callback
          user-info-uri: https://www.googleapis.com/oauth2/v3/userinfo
```

## Quy Trình Hoạt Động

1. **User click "Đăng nhập bằng Google"**
   - Frontend gọi `/auth/google/login`
   - Backend redirect đến trang đăng nhập Google

2. **User đăng nhập Google**
   - User nhập thông tin Google của họ
   - Google xác thực và yêu cầu phân quyền

3. **Google redirect về callback**
   - Google gửi authorization code về `/auth/google/callback`
   - Backend nhận code và gọi Google API để lấy access token

4. **Backend xử lý thông tin user**
   - Lấy thông tin user từ Google
   - Tìm kiếm user trong database:
     - Nếu tìm thấy theo `googleId`: Cập nhật thông tin
     - Nếu tìm thấy theo `email`: Link Google account với user hiện tại
     - Nếu không tìm thấy: Tạo user mới

5. **Trả về JWT tokens**
   - Backend tạo access_token và refresh_token
   - Lưu refresh_token vào cookie HTTP-only
   - Trả về response cho frontend

## Response Format

```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "user": {
    "userId": "uuid",
    "username": "username",
    "email": "user@gmail.com",
    "fullName": "Full Name",
    "role": "USER",
    "isActive": true,
    "emailVerified": true
  }
}
```

## Tích Hợp Frontend

### Web Application (React/Angular/Vue)

```javascript
// Redirect đến Google login
window.location.href = 'http://localhost:8080/auth/google/login';

// Hoặc xử lý callback
const urlParams = new URLSearchParams(window.location.search);
const code = urlParams.get('code');

if (code) {
  fetch(`http://localhost:8080/auth/google/callback?code=${code}`)
    .then(response => response.json())
    .then(data => {
      // Lưu access token
      localStorage.setItem('accessToken', data.accessToken);
      // Redirect đến trang chính
      window.location.href = '/home';
    });
}
```

### Mobile Application

```javascript
// Lấy authorization code từ Google SDK
const code = await GoogleSignIn.getAuthorizationCode();

// Gửi code đến backend
const response = await fetch('http://localhost:8080/auth/google/authenticate?code=' + code, {
  method: 'POST'
});

const data = await response.json();
// Lưu access token
```

## Bảo Mật

1. **JWT Tokens**: Sử dụng HS512 algorithm
2. **Refresh Token**: Được lưu trong HTTP-only cookie để tránh XSS
3. **HTTPS**: Nên sử dụng HTTPS trong production
4. **Client Secret**: Không bao giờ expose client secret ra frontend

## Lưu Ý

- User đăng nhập lần đầu qua Google sẽ được tạo tài khoản tự động
- Email từ Google được đánh dấu là đã xác thực (`emailVerified = true`)
- Username sẽ được tự động tạo từ email (phần trước @)
- Nếu username đã tồn tại, hệ thống sẽ thêm số đếm (vd: username1, username2)
- User có thể đăng nhập bằng cả Google và password thông thường

## Testing

### Test với Postman

1. **Test redirect login**:
   - Method: GET
   - URL: `http://localhost:8080/auth/google/login`
   - Sẽ redirect đến Google

2. **Test callback** (sau khi có code từ Google):
   - Method: GET
   - URL: `http://localhost:8080/auth/google/callback?code=YOUR_CODE`
   - Hoặc method POST với `/auth/google/authenticate?code=YOUR_CODE`

## Troubleshooting

### Lỗi "redirect_uri_mismatch"
- Kiểm tra lại redirect URI trong Google Console phải khớp với config
- Phải thêm `http://localhost:8080/auth/google/callback` vào Authorized redirect URIs

### Lỗi "invalid_client"
- Kiểm tra client_id và client_secret
- Đảm bảo Google OAuth credentials còn hiệu lực

### Lỗi "GOOGLE_AUTH_ERROR"
- Kiểm tra logs để xem chi tiết lỗi
- Đảm bảo có kết nối internet để gọi Google API

## Cấu Hình Production

Khi deploy lên production, cần cập nhật:

```yaml
spring:
  security:
    oauth2:
      registration:
        google:
          redirect-uri: "https://yourdomain.com/auth/google/callback"
```

Và thêm domain production vào Google Console Authorized redirect URIs.

## Contact

Nếu có vấn đề, vui lòng liên hệ team phát triển.

