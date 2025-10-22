# Hướng Dẫn Đăng Nhập Bằng Facebook

## Tổng Quan
Chức năng đăng nhập bằng Facebook đã được triển khai hoàn chỉnh cho ứng dụng CookShare.

## Các Thành Phần Đã Tạo

### 1. DTO (Data Transfer Objects)
- **FacebookTokenResponse.java**: Nhận access token từ Facebook
- **FacebookUserInfo.java**: Chứa thông tin người dùng từ Facebook (id, email, name, picture)

### 2. Service Layer
- **FacebookOAuthService.java**: Interface định nghĩa các method
- **FacebookOAuthServiceImpl.java**: Implementation xử lý OAuth flow với Facebook

### 3. Controller
- **FacebookAuthController.java**: Xử lý các endpoint liên quan đến Facebook authentication

### 4. Error Handling
- **ErrorCode.FACEBOOK_AUTH_ERROR**: Mã lỗi 5002 cho các lỗi xác thực Facebook

## API Endpoints

### 1. Redirect đến trang đăng nhập Facebook
```
GET http://localhost:8080/auth/facebook/login
```
**Mô tả**: Chuyển hướng người dùng đến trang đăng nhập Facebook

**Response**: Redirect 302 đến Facebook OAuth page

---

### 2. Callback từ Facebook (Web)
```
GET http://localhost:8080/auth/facebook/callback?code={authorization_code}
```
**Mô tả**: Facebook sẽ redirect về endpoint này sau khi user đăng nhập thành công

**Query Parameters**:
- `code`: Authorization code từ Facebook
- `error` (optional): Error code nếu có lỗi
- `error_description` (optional): Mô tả lỗi

**Response**:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "user": {
    "userId": "uuid",
    "username": "username",
    "email": "user@example.com",
    "fullName": "User Name",
    "role": "USER",
    "isActive": true,
    "emailVerified": true
  }
}
```

---

### 3. Authenticate với authorization code (Mobile/Frontend)
```
POST http://localhost:8080/auth/facebook/authenticate?code={authorization_code}
```
**Mô tả**: Endpoint cho mobile app hoặc SPA để đổi authorization code lấy JWT tokens

**Query Parameters**:
- `code`: Authorization code từ Facebook

**Response**: Giống như callback endpoint

## Flow Đăng Nhập

### Web Flow:
1. User click "Đăng nhập bằng Facebook" → Gọi `GET /auth/facebook/login`
2. Backend redirect user đến Facebook OAuth page
3. User đăng nhập và cấp quyền trên Facebook
4. Facebook redirect về `GET /auth/facebook/callback?code=xxx`
5. Backend nhận code, đổi lấy access token từ Facebook
6. Backend lấy thông tin user từ Facebook
7. Backend tìm hoặc tạo user trong database
8. Backend tạo JWT tokens và trả về cho client

### Mobile/SPA Flow:
1. Frontend tự xử lý OAuth flow với Facebook và nhận được authorization code
2. Frontend gọi `POST /auth/facebook/authenticate?code=xxx`
3. Backend xử lý tương tự như web flow
4. Frontend nhận JWT tokens

## Xử Lý User

### Tìm User Hiện Tại:
1. Tìm theo `facebookId` trước
2. Nếu không tìm thấy, tìm theo `email` (nếu Facebook cung cấp)
3. Nếu tìm thấy user theo email → Link Facebook account với user hiện có
4. Nếu không tìm thấy → Tạo user mới

### Tạo User Mới:
- **Username**: Generate từ email (vd: `nhat6a4`, `nhat6a41`, `nhat6a42`,...)
- **Email**: Sử dụng email từ Facebook, hoặc tạo email giả nếu Facebook không cung cấp (`facebook_{id}@cookshare.local`)
- **Password**: Set là "FACEBOOK_AUTH" (không dùng được để đăng nhập thông thường)
- **Avatar**: Sử dụng ảnh đại diện từ Facebook
- **EmailVerified**: true (Facebook đã verify)

## Cấu Hình

File: `src/main/resources/application.yml`

```yaml
spring:
  security:
    oauth2:
      registration:
        facebook:
          client-id: 828224186245470
          client-secret: 8068a1f8cb3f07b3d861feee491d0490
          redirect-uri: http://localhost:8080/auth/facebook/callback
          auth-uri: https://www.facebook.com/v21.0/dialog/oauth
          token-uri: https://graph.facebook.com/oauth/access_token
          user-info-uri: https://graph.facebook.com/me?fields=id,name,email,picture.type(large)
          scope: email,public_profile
```

### Các Quyền Yêu Cầu:
- `email`: Lấy địa chỉ email của user
- `public_profile`: Lấy thông tin cơ bản (id, name, picture)

## Security

### JWT Tokens:
- **Access Token**: Thời hạn 24 giờ (86400 seconds)
- **Refresh Token**: Thời hạn 100 ngày (8640000 seconds)
- Refresh token được lưu trong HTTP-only cookie để bảo mật

### Xử Lý Lỗi:
- Tất cả lỗi liên quan đến Facebook đều trả về `ErrorCode.FACEBOOK_AUTH_ERROR` (5002)
- Log chi tiết lỗi để debug

## Testing

### Test bằng Browser:
1. Mở browser và truy cập: `http://localhost:8080/auth/facebook/login`
2. Đăng nhập Facebook
3. Kiểm tra response trả về có JWT tokens

### Test bằng Postman:
1. Lấy authorization code từ Facebook OAuth manually
2. Gọi `POST http://localhost:8080/auth/facebook/authenticate?code={code}`
3. Verify response

## Lưu Ý

### Email Không Có:
- Một số user Facebook không cung cấp email
- System sẽ tạo email giả: `facebook_{facebook_id}@cookshare.local`
- User vẫn có thể đăng nhập và sử dụng app bình thường

### Link Accounts:
- Nếu user đã có account với email X
- User đăng nhập Facebook với cùng email X
- System sẽ tự động link Facebook account với user hiện có
- User có thể dùng cả 2 cách đăng nhập

### Facebook App Configuration:
Đảm bảo trong Facebook Developer Console:
1. Thêm redirect URI: `http://localhost:8080/auth/facebook/callback`
2. Enable Email permission
3. App ở chế độ Development hoặc Live

## Điểm Khác Biệt với Google Login

| Feature | Google | Facebook |
|---------|--------|----------|
| Token Endpoint | POST với form data | GET với query params |
| User Info URL | Bearer token in header | Access token in URL |
| Email | Luôn có | Có thể không có |
| Picture | Direct URL | Nested object |

## Troubleshooting

### Lỗi "redirect_uri_mismatch":
- Kiểm tra redirect URI trong Facebook App Settings
- Phải khớp chính xác với giá trị trong `application.yml`

### Lỗi "invalid_code":
- Authorization code chỉ dùng được 1 lần
- Code có thời hạn ngắn, phải sử dụng ngay

### Không nhận được email:
- Kiểm tra permissions trong Facebook App
- User có thể không cấp quyền email
- System sẽ tạo email giả để user vẫn đăng nhập được

