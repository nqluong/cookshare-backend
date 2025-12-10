package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.request.ResetPasswordRequest;
import com.backend.cookshare.authentication.service.ForgotPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/forgotPassword")
public class ForgotPasswordController {
    private final ForgotPasswordService forgotPasswordService;

    //send email for verification
    @PostMapping("/verifyMail/{email}")
    public ResponseEntity<String> verifyEmail(@PathVariable String email) {
        String message = forgotPasswordService.sendOtpForPasswordReset(email);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/verifyOtp/{email}/{otp}")
    public ResponseEntity<String> verifyOtp(@PathVariable String email, @PathVariable Integer otp) {
        String message = forgotPasswordService.verifyOtpForPasswordReset(email, otp);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/resetPassword/{email}")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest, @PathVariable String email) {
        String message = forgotPasswordService.resetPassword(email, resetPasswordRequest);
        return ResponseEntity.ok(message);
    }
}
