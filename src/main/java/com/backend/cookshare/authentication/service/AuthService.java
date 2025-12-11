package com.backend.cookshare.authentication.service;

import com.backend.cookshare.authentication.dto.LoginDTO;
import com.backend.cookshare.authentication.dto.request.ChangePasswordRequest;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    /**
     * Đăng ký tài khoản mới
     */
    String register(UserRequest userRequest);

    /**
     * Đăng nhập
     */
    LoginResponseDTO login(LoginDTO loginDto);

    /**
     * Lấy thông tin tài khoản hiện tại
     */
    LoginResponseDTO.UserInfo getAccount();

    /**
     * Làm mới access token bằng refresh token
     */
    LoginResponseDTO refreshToken(String refreshToken);

    /**
     * Đăng xuất
     */
    void logout(HttpServletRequest request);

    /**
     * Đổi mật khẩu
     */
    void changePassword(ChangePasswordRequest request);
}

