# Tổ Chức Lại Code - Service Layer Refactoring

## Tổng Quan
Đã tổ chức lại code để xử lý logic nghiệp vụ ở tầng Service thay vì trong Controller. Điều này giúp:
- **Tách biệt trách nhiệm**: Controller chỉ xử lý HTTP requests/responses, Service xử lý business logic
- **Tái sử dụng code**: Logic có thể được dùng lại ở nhiều nơi
- **Dễ kiểm thử**: Service layer dễ unit test hơn
- **Bảo trì tốt hơn**: Code rõ ràng, dễ đọc và dễ maintain

## Các Service Mới Được Tạo

### 1. AuthService
**File**: `authentication/service/AuthService.java` và `impl/AuthServiceImpl.java`

**Chức năng**:
- `register()`: Đăng ký tài khoản mới
- `login()`: Đăng nhập và tạo JWT tokens
- `getAccount()`: Lấy thông tin tài khoản hiện tại
- `refreshToken()`: Làm mới access token
- `logout()`: Đăng xuất và blacklist token
- `changePassword()`: Đổi mật khẩu

**Logic đã chuyển từ Controller**:
- Xác thực người dùng với Spring Security
- Kiểm tra tài khoản có bị khóa không
- Tạo và quản lý JWT tokens
- Cập nhật refresh token trong database
- Blacklist token khi logout
- Validate và cập nhật mật khẩu

### 2. EmailVerificationService
**File**: `authentication/service/EmailVerificationService.java` và `impl/EmailVerificationServiceImpl.java`

**Chức năng**:
- `sendVerificationOtp()`: Gửi OTP để xác thực email
- `verifyOtp()`: Xác thực OTP và đánh dấu email đã verified

**Logic đã chuyển từ Controller**:
- Lấy user từ SecurityContext
- Kiểm tra email đã verified chưa
- Generate OTP 6 chữ số ngẫu nhiên
- Gửi email HTML với template Thymeleaf
- Xác thực OTP và expiration time
- Cập nhật trạng thái emailVerified
- Mask email để bảo mật

### 3. ForgotPasswordService
**File**: `authentication/service/ForgotPasswordService.java` và `impl/ForgotPasswordServiceImpl.java`

**Chức năng**:
- `sendOtpForPasswordReset()`: Gửi OTP qua email
- `verifyOtpForPasswordReset()`: Xác thực OTP
- `resetPassword()`: Reset mật khẩu mới

**Logic đã chuyển từ Controller**:
- Validate email tồn tại
- Generate và lưu OTP
- Gửi email với template
- Xác thực OTP và đánh dấu đã verified
- Kiểm tra password mismatch
- Mã hóa và cập nhật password mới
- Xóa OTP sau khi reset thành công

### 4. UserProfileService
**File**: `authentication/service/UserProfileService.java` và `impl/UserProfileServiceImpl.java`

**Chức năng**:
- `getUserProfileById()`: Lấy profile theo user ID
- `getUserProfileByUsername()`: Lấy profile theo username
- `updateUserProfile()`: Cập nhật profile
- `generateAvatarUploadUrl()`: Tạo URL upload avatar

**Logic đã chuyển từ Controller**:
- Build UserProfileDto từ User entity
- Tính tổng số likes từ recipes
- Aggregate dữ liệu từ nhiều nguồn (User, Recipe)
- Xử lý avatar upload URL generation

## Các Controller Đã Được Refactor

### 1. AuthController
**Trước**:
```java
@PostMapping("/auth/login")
public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDto) {
    // 50+ dòng code xử lý authentication, tokens, cookies...
}
```

**Sau**:
```java
@PostMapping("/auth/login")
public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDto) {
    LoginResponseDTO response = authService.login(loginDto);
    response.setExpiresIn(accessTokenExpiration);
    
    // Chỉ xử lý HTTP concerns (cookies)
    ResponseCookie resCookies = ResponseCookie
        .from("refresh_token", response.getRefreshToken())
        .httpOnly(true).secure(true).path("/")
        .maxAge(refreshTokenExpiration).build();
    
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, resCookies.toString())
        .body(response);
}
```

### 2. EmailVerificationController
**Trước**: 40+ dòng code trong mỗi endpoint
**Sau**: Mỗi endpoint chỉ 2-3 dòng gọi service

```java
@PostMapping("/send-otp")
public ResponseEntity<String> sendVerificationOtp() {
    String message = emailVerificationService.sendVerificationOtp();
    return ResponseEntity.ok(message);
}
```

### 3. ForgotPasswordController
Tương tự, đã được refactor để gọi service thay vì xử lý logic trực tiếp

### 4. UserController
**Trước**: Build UserProfileDto với nhiều dòng code lặp lại
**Sau**: Sử dụng UserProfileService để xử lý

```java
@GetMapping("/{userId}")
public ResponseEntity<UserProfileDto> getUserById(@PathVariable UUID userId) {
    return userProfileService.getUserProfileById(userId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}
```

## Lợi Ích Đạt Được

### 1. Separation of Concerns
- **Controller**: Chỉ xử lý HTTP layer (request/response, status codes, headers, cookies)
- **Service**: Xử lý toàn bộ business logic và validation
- **Repository**: Chỉ truy cập database

### 2. Code Reusability
- Logic trong service có thể được sử dụng từ nhiều controller
- Logic có thể được gọi từ scheduled tasks, events, hoặc các service khác

### 3. Testability
- Service layer dễ viết unit test hơn (mock dependencies)
- Không cần mock HTTP requests khi test business logic

### 4. Maintainability
- Code rõ ràng hơn, dễ đọc hơn
- Thay đổi business logic chỉ cần sửa ở service
- Controller gọn gàng, dễ hiểu flow

### 5. Transaction Management
- Service layer có thể dễ dàng áp dụng `@Transactional`
- Quản lý transaction tốt hơn cho các operation phức tạp

## Best Practices Được Áp Dụng

1. **Interface-based Design**: Mỗi service có interface và implementation
2. **Dependency Injection**: Sử dụng constructor injection với `@RequiredArgsConstructor`
3. **Logging**: Thêm log ở tầng service để track business operations
4. **Exception Handling**: Throw custom exceptions từ service, được xử lý bởi global exception handler
5. **Single Responsibility**: Mỗi service chỉ xử lý một domain cụ thể
6. **Transaction Management**: Sử dụng `@Transactional` ở service layer

## Cấu Trúc Thư Mục

```
authentication/
├── controller/              # HTTP layer (thin)
│   ├── AuthController.java
│   ├── EmailVerificationController.java
│   ├── ForgotPasswordController.java
│   └── UserController.java
├── service/                 # Business logic (thick)
│   ├── AuthService.java
│   ├── EmailVerificationService.java
│   ├── ForgotPasswordService.java
│   ├── UserProfileService.java
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── EmailVerificationServiceImpl.java
│       ├── ForgotPasswordServiceImpl.java
│       └── UserProfileServiceImpl.java
├── repository/              # Data access
├── dto/                     # Data transfer objects
└── entity/                  # Domain models
```

## Các File Mới

1. `AuthService.java` - Interface
2. `AuthServiceImpl.java` - Implementation
3. `EmailVerificationService.java` - Interface
4. `EmailVerificationServiceImpl.java` - Implementation
5. `ForgotPasswordService.java` - Interface
6. `ForgotPasswordServiceImpl.java` - Implementation
7. `UserProfileService.java` - Interface
8. `UserProfileServiceImpl.java` - Implementation
9. `OAuthService.java` - Interface ⭐ NEW
10. `OAuthServiceImpl.java` - Implementation ⭐ NEW

## Các File Đã Được Sửa Đổi

1. `AuthController.java` - Refactored to use AuthService
2. `EmailVerificationController.java` - Refactored to use EmailVerificationService
3. `ForgotPasswordController.java` - Refactored to use ForgotPasswordService
4. `UserController.java` - Refactored to use UserProfileService
5. `GoogleAuthController.java` - Refactored to use OAuthService ⭐ NEW
6. `FacebookAuthController.java` - Refactored to use OAuthService ⭐ NEW
7. `ErrorCode.java` - Added INVALID_OAUTH_PROVIDER error code ⭐ NEW

## Kiểm Tra

Sau khi refactor, cần kiểm tra:
- ✅ Code compile thành công
- ✅ Các endpoint API vẫn hoạt động đúng
- ✅ Authentication flow hoạt động bình thường
- ✅ Email verification flow hoạt động
- ✅ Forgot password flow hoạt động
- ✅ User profile endpoints hoạt động

## Tài Liệu Tham Khảo

- Spring Boot Service Layer Best Practices
- Clean Architecture principles
- Domain-Driven Design (DDD)

