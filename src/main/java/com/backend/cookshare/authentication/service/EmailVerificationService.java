package com.backend.cookshare.authentication.service;

public interface EmailVerificationService {
    /**
     * Gửi OTP để xác thực email
     */
    String sendVerificationOtp();

    /**
     * Xác thực OTP
     */
    String verifyOtp(Integer otp);
}

