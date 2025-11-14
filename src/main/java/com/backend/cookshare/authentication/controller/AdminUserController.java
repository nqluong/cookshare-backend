package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.request.AdminUpdateUserRequest;
import com.backend.cookshare.authentication.dto.request.BanUserRequest;
import com.backend.cookshare.authentication.dto.response.AdminUserDetailResponseDTO;
import com.backend.cookshare.authentication.dto.response.AdminUserListResponseDTO;
import com.backend.cookshare.authentication.service.AdminUserService;
import com.backend.cookshare.common.dto.ApiResponse;
import com.backend.cookshare.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * GET /api/admin/users - Lấy danh sách tất cả người dùng với phân trang và tìm kiếm
     * @param search Từ khóa tìm kiếm (username, email, fullName) - có thể null
     * @param page Số trang (mặc định: 0)
     * @param size Kích thước trang (mặc định: 10)
     * @param sortBy Trường sắp xếp (mặc định: createdAt)
     * @param sortDir Hướng sắp xếp (mặc định: desc)
     * @return Danh sách người dùng có phân trang
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserListResponseDTO>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Admin đang lấy danh sách tất cả người dùng - trang: {}, kích thước: {}, tìm kiếm: {}", page, size, search);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : 
                    Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<AdminUserListResponseDTO> pageResponse = adminUserService.getAllUsersWithPagination(search, pageable);

        ApiResponse<PageResponse<AdminUserListResponseDTO>> response = ApiResponse.<PageResponse<AdminUserListResponseDTO>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách người dùng thành công")
                .data(pageResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/users/{id} - Lấy thông tin chi tiết người dùng
     * @param id ID của người dùng
     * @return Thông tin chi tiết người dùng
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponseDTO>> getUserDetail(@PathVariable UUID id) {
        log.info("Admin đang lấy thông tin chi tiết người dùng với userId: {}", id);

        AdminUserDetailResponseDTO userDetail = adminUserService.getUserDetailById(id);

        ApiResponse<AdminUserDetailResponseDTO> response = ApiResponse.<AdminUserDetailResponseDTO>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin người dùng thành công")
                .data(userDetail)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/users/{id}/ban - Cấm người dùng
     * @param id ID của người dùng
     * @param request Yêu cầu cấm với lý do (có thể null)
     * @return Phản hồi thành công
     */
    @PutMapping("/{id}/ban")
    public ResponseEntity<ApiResponse<Void>> banUser(
            @PathVariable UUID id,
            @RequestBody(required = false) BanUserRequest request) {
        
        log.info("Admin đang cấm người dùng: {}", id);
        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Không có lý do";
        
        adminUserService.banUser(id, reason);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Cấm người dùng thành công")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/users/{id}/unban - Gỡ cấm người dùng
     * @param id ID của người dùng
     * @return Phản hồi thành công
     */
    @PutMapping("/{id}/unban")
    public ResponseEntity<ApiResponse<Void>> unbanUser(@PathVariable UUID id) {
        log.info("Admin đang gỡ cấm người dùng: {}", id);
        
        adminUserService.unbanUser(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Gỡ cấm người dùng thành công")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/admin/users/{id} - Xóa người dùng
     * @param id ID của người dùng
     * @return Phản hồi thành công
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        log.info("Admin đang xóa người dùng: {}", id);
        
        adminUserService.deleteUser(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Xóa người dùng thành công")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/users/{id} - Cập nhật thông tin người dùng (dành cho admin)
     * @param id ID của người dùng
     * @param request Thông tin cập nhật
     * @return Thông tin người dùng đã được cập nhật
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponseDTO>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        
        log.info("Admin đang cập nhật thông tin người dùng: {}", id);
        
        AdminUserDetailResponseDTO updatedUser = adminUserService.updateUser(id, request);

        ApiResponse<AdminUserDetailResponseDTO> response = ApiResponse.<AdminUserDetailResponseDTO>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Cập nhật thông tin người dùng thành công")
                .data(updatedUser)
                .build();

        return ResponseEntity.ok(response);
    }

}

