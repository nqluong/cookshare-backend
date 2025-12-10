# OAuth Service Refactoring - Chi Tiết

## 🎯 Mục Tiêu
Tách biệt business logic của OAuth authentication (Google & Facebook) ra khỏi Controller để code dễ maintain và test hơn.

## 📦 Service Mới: OAuthService

### Interface: `OAuthService.java`
```java
public interface OAuthService {
    // Xác thực với OAuth provider (Google/Facebook)
    LoginResponseDTO authenticateWithOAuth(String code, String provider);
    
    // Quản lý kết quả authentication cho polling
    void saveAuthResult(String state, LoginResponseDTO result);
    void saveAuthError(String state, String errorCode, String errorMessage);
    LoginResponseDTO getAuthResult(String state);
    Map<String, Object> getAuthError(String state);
    
    // Auto-cleanup
    void scheduleResultRemoval(String state, long delayMillis);
}
```

### Implementation: `OAuthServiceImpl.java`

**Chức năng chính**:

1. **authenticateWithOAuth()**
   - Xác thực với Google hoặc Facebook OAuth service
   - Kiểm tra tài khoản có bị khóa không
   - Cập nhật last active time
   - Trả về LoginResponseDTO với JWT tokens

2. **saveAuthResult() / saveAuthError()**
   - Lưu trữ tạm thời kết quả authentication (dùng ConcurrentHashMap)
   - Tự động schedule cleanup sau 5 phút để tránh memory leak

3. **getAuthResult() / getAuthError()**
   - Lấy kết quả để trả về cho client khi polling
   - Error result bị xóa ngay sau khi lấy
   - Success result schedule xóa sau 30s (tránh race condition)

4. **Auto-cleanup mechanism**
   - Xóa result tự động sau 5 phút
   - Xóa result sau 30s khi đã được fetch
   - Tránh memory leak khi client không poll

**Dependencies**:
- `GoogleOAuthService` - Xác thực với Google
- `FacebookOAuthService` - Xác thực với Facebook  
- `UserService` - Cập nhật user info
- `SecurityUtil` - Tạo JWT tokens

## 🔄 Controllers Đã Refactor

### GoogleAuthController - TRƯỚC

```java
@Controller
public class GoogleAuthController {
    private final GoogleOAuthService googleOAuthService;
    
    // ❌ Quản lý state/result trong controller
    private final Map<String, LoginResponseDTO> authResults = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> authErrors = new ConcurrentHashMap<>();
    
    @GetMapping("/callback")
    public Object googleCallback(...) {
        // ❌ Business logic trong controller
        LoginResponseDTO response = googleOAuthService.authenticateGoogleUser(code);
        
        // ❌ Kiểm tra user active trong controller
        if (!response.getUser().getIsActive()) {
            throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
        }
        
        // ❌ Quản lý state result trong controller
        authResults.put(state, response);
        scheduleResultCleanup(state);
        
        // Set cookie và return view...
    }
    
    // ❌ Helper methods trong controller
    private void scheduleResultCleanup(String state) { ... }
    private void scheduleErrorCleanup(String state) { ... }
    private void scheduleResultRemoval(String state, long delay) { ... }
}
```

### GoogleAuthController - SAU

```java
@Controller
public class GoogleAuthController {
    private final OAuthService oAuthService; // ✅ Chỉ inject service
    
    @GetMapping("/callback")
    public Object googleCallback(...) {
        // ✅ Chỉ gọi service, không có business logic
        LoginResponseDTO response = oAuthService.authenticateWithOAuth(code, "google");
        
        // ✅ Service xử lý toàn bộ
        oAuthService.saveAuthResult(state, response);
        
        // ✅ Controller chỉ xử lý HTTP concerns (cookies, view)
        ResponseCookie refreshCookie = ResponseCookie.from(...)...
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        
        model.addAttribute("state", state);
        model.addAttribute("provider", "google");
        return "auth-loading";
    }
}
```

### FacebookAuthController
Refactoring tương tự GoogleAuthController:
- ✅ Xóa bỏ ConcurrentHashMap trong controller
- ✅ Xóa bỏ tất cả helper methods
- ✅ Chỉ gọi OAuthService
- ✅ Controller chỉ xử lý HTTP layer

## 📊 So Sánh Trước/Sau

### Trước Refactor
```
Controller (200+ lines)
├── Business Logic (50 lines)
│   ├── Authenticate user
│   ├── Check user active
│   ├── Update last active
│   └── Manage state/results
├── Helper Methods (80 lines)
│   ├── scheduleResultCleanup()
│   ├── scheduleErrorCleanup()
│   └── scheduleResultRemoval()
└── HTTP Logic (70 lines)
    ├── Set cookies
    ├── Build response
    └── Return views
```

### Sau Refactor
```
Controller (50 lines) ✅ Thin
└── HTTP Logic only
    ├── Call service
    ├── Set cookies
    └── Return views

Service (150 lines) ✅ Thick
├── Business Logic
│   ├── authenticateWithOAuth()
│   ├── Check user active
│   └── Update last active
└── Result Management
    ├── Save/Get results
    ├── Save/Get errors
    └── Auto-cleanup
```

## 🎁 Lợi Ích

### 1. **Single Responsibility**
- Controller: Chỉ xử lý HTTP (cookies, redirects, views)
- Service: Xử lý business logic (authentication, state management)

### 2. **Code Reusability**
- OAuth logic có thể được gọi từ nhiều nơi
- Không bị duplicate code giữa Google và Facebook

### 3. **Testability**
```java
// Trước: Phải mock HTTP request/response
@Test
void testGoogleCallback() {
    // Mock HttpServletResponse, Model, etc...
}

// Sau: Chỉ cần test service với mock dependencies
@Test
void testAuthenticateWithOAuth() {
    when(googleOAuthService.authenticate(code)).thenReturn(userInfo);
    LoginResponseDTO result = oAuthService.authenticateWithOAuth(code, "google");
    verify(userService).updateUser(any());
}
```

### 4. **Maintainability**
- Thay đổi business logic chỉ cần sửa service
- Controller rất gọn, dễ đọc
- Dễ thêm OAuth provider mới (LinkedIn, GitHub...)

### 5. **Centralized State Management**
- Tất cả state/result management ở một nơi
- Dễ chuyển sang Redis khi scale
- Consistent cleanup logic

## 🚀 Mở Rộng Trong Tương Lai

### Dễ dàng thêm OAuth provider mới:
```java
// 1. Tạo service mới
@Service
class LinkedInOAuthService { ... }

// 2. Inject vào OAuthServiceImpl
private final LinkedInOAuthService linkedInOAuthService;

// 3. Thêm case mới
public LoginResponseDTO authenticateWithOAuth(String code, String provider) {
    if ("linkedin".equalsIgnoreCase(provider)) {
        return linkedInOAuthService.authenticate(code);
    }
    // ...
}

// 4. Controller tự động hoạt động!
```

### Dễ chuyển sang Redis:
```java
@Service
class OAuthServiceImpl {
    private final RedisTemplate<String, LoginResponseDTO> redisTemplate;
    
    public void saveAuthResult(String state, LoginResponseDTO result) {
        redisTemplate.opsForValue().set("oauth:" + state, result, 5, TimeUnit.MINUTES);
    }
}
```

## ✅ Checklist

- [x] Tạo OAuthService interface
- [x] Tạo OAuthServiceImpl
- [x] Refactor GoogleAuthController
- [x] Refactor FacebookAuthController
- [x] Xóa duplicate code
- [x] Thêm error code INVALID_OAUTH_PROVIDER
- [x] Update documentation
- [x] Code compile thành công
- [ ] Test các OAuth flows
- [ ] Test polling mechanism
- [ ] Test auto-cleanup

## 📝 Notes

**⚠️ Trong Production**:
- Nên dùng Redis thay vì ConcurrentHashMap để lưu state/results
- Có thể scale horizontally khi dùng Redis
- Thêm monitoring cho cleanup operations

**🔒 Security**:
- State parameter đã được validate
- OTP/Token có expiration time
- Auto-cleanup tránh memory leak

**📊 Metrics có thể thêm**:
- Số lượng OAuth requests
- Success/failure rate
- Average authentication time
- State cleanup effectiveness

