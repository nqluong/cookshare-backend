package com.backend.cookshare.authentication.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @Size(min = 3, max = 100, message = "Username phải có từ 3 đến 100 ký tự")
    private String username;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(max = 255, message = "Tên đầy đủ không được vượt quá 255 ký tự")
    private String fullName;

    private String avatarUrl;

    @Size(max = 1000, message = "Bio không được vượt quá 1000 ký tự")
    private String bio;
}

