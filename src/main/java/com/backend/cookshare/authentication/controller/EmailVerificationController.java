package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email-verification")
@Slf4j
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    /**
     * Gửi OTP để xác thực email
     */
    @PostMapping("/send-otp")
    public ResponseEntity<String> sendVerificationOtp() {
        String message = emailVerificationService.sendVerificationOtp();
        return ResponseEntity.ok(message);
    }

    /**
     * Xác thực OTP
     */
    @PostMapping("/verify-otp/{otp}")
    public ResponseEntity<String> verifyOtp(@PathVariable Integer otp) {
        String message = emailVerificationService.verifyOtp(otp);
        return ResponseEntity.ok(message);
    }
}
