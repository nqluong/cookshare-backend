package com.backend.cookshare.authentication.dto.request;

import com.backend.cookshare.authentication.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho yêu cầu cập nhật thông tin người dùng bởi Admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {

    @Size(min = 3, max = 100, message = "Username phải có từ 3 đến 100 ký tự")
    private String username;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(max = 255, message = "Tên đầy đủ không được vượt quá 255 ký tự")
    private String fullName;

    private String avatarUrl;

    @Size(max = 1000, message = "Bio không được vượt quá 1000 ký tự")
    private String bio;

    @NotNull(message = "Role không được để trống")
    private UserRole role;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive;

    @NotNull(message = "Trạng thái xác thực email không được để trống")
    private Boolean emailVerified;
}
