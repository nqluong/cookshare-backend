package com.backend.cookshare.authentication.service;

import com.backend.cookshare.authentication.dto.response.AdminUserDetailResponseDTO;
import com.backend.cookshare.authentication.dto.response.AdminUserListResponseDTO;
import com.backend.cookshare.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    
    /**
     * Lấy danh sách tất cả người dùng với phân trang và tìm kiếm
     * @param search Từ khóa tìm kiếm (có thể null)
     * @param pageable Đối tượng phân trang và sắp xếp
     * @return PageResponse chứa danh sách AdminUserListResponseDTO
     */
    PageResponse<AdminUserListResponseDTO> getAllUsersWithPagination(String search, Pageable pageable);
    
    /**
     * Lấy thông tin chi tiết người dùng theo ID
     * @param userId ID của người dùng
     * @return AdminUserDetailResponseDTO chứa thông tin chi tiết
     */
    AdminUserDetailResponseDTO getUserDetailById(java.util.UUID userId);
    
    /**
     * Cấm người dùng
     * @param userId ID của người dùng
     * @param reason Lý do cấm
     */
    void banUser(java.util.UUID userId, String reason);
    
    /**
     * Gỡ cấm người dùng
     * @param userId ID của người dùng
     */
    void unbanUser(java.util.UUID userId);
    
    /**
     * Xóa người dùng (chỉ dành cho admin)
     * @param userId ID của người dùng
     */
    void deleteUser(java.util.UUID userId);
    
    /**
     * Cập nhật thông tin người dùng bởi Admin
     * @param userId ID của người dùng
     * @param request Yêu cầu cập nhật thông tin
     * @return AdminUserDetailResponseDTO chứa thông tin đã cập nhật
     */
    AdminUserDetailResponseDTO updateUser(java.util.UUID userId, com.backend.cookshare.authentication.dto.request.AdminUpdateUserRequest request);
}
