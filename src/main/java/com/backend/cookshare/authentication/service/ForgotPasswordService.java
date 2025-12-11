package com.backend.cookshare.authentication.service;

import com.backend.cookshare.authentication.dto.request.ResetPasswordRequest;

public interface ForgotPasswordService {
    /**
     * Gửi OTP qua email để reset password
     */
    String sendOtpForPasswordReset(String email);

    /**
     * Xác thực OTP
     */
    String verifyOtpForPasswordReset(String email, Integer otp);

    /**
     * Reset password
     */
    String resetPassword(String email, ResetPasswordRequest request);
}

