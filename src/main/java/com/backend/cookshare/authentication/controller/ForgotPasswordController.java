package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.request.ResetPasswordRequest;
import com.backend.cookshare.authentication.entity.ForgotPassword;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.ForgotPasswordRepository;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.EmailService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.Date;
import java.util.Random;

@RestController
@RequiredArgsConstructor
@RequestMapping("/forgotPassword")
public class ForgotPasswordController {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final PasswordEncoder passwordEncoder;

    //send email for verification
    @PostMapping("/verifyMail/{email}")
    public ResponseEntity<String> verifyEmail(@PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_EMAIL));

        Integer otp = otpGenerator();

        // Xóa tất cả OTP cũ của user trước khi tạo mới
        forgotPasswordRepository.deleteByUser(user);

        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 60 * 1000 * 5))
                .user(user)
                .build();

        try {
            // Tạo context cho Thymeleaf template
            Context context = new Context();
            context.setVariable("username", user.getFullName() != null ? user.getFullName() : user.getUsername());
            context.setVariable("otp", otp);

            // Gửi email HTML
            emailService.sendHtmlMessage(
                    email,
                    "CookShare - Xác thực OTP đặt lại mật khẩu",
                    "otp-email",
                    context
            );

            forgotPasswordRepository.save(fp);
            return ResponseEntity.ok("Xác minh email đã được gửi tới: " + email);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @PostMapping("/verifyOtp/{email}/{otp}")
    public ResponseEntity<String> verifyOtp(@PathVariable String email, @PathVariable Integer otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_EMAIL));

        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new CustomException(ErrorCode.OTP_NOT_FOUND));

        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteByUser(user);
            throw new CustomException(ErrorCode.OTP_EXPIRED);
        }

        // Đánh dấu OTP đã được xác thực
        fp.setIsVerified(true);
        forgotPasswordRepository.save(fp);

        return ResponseEntity.ok("OTP hợp lệ! Bạn có thể đặt lại mật khẩu.");
    }

    @PostMapping("/resetPassword/{email}")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest, @PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_EMAIL));

        // Kiểm tra xem OTP đã được xác thực chưa
        ForgotPassword fp = forgotPasswordRepository.findByUserAndIsVerified(user, true)
                .orElseThrow(() -> new CustomException(ErrorCode.OTP_NOT_VERIFIED));

        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteByUser(user);
            throw new CustomException(ErrorCode.OTP_EXPIRED);
        }

        if (!resetPasswordRequest.getNewPassword().equals(resetPasswordRequest.getConfirmPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // Mã hóa và cập nhật mật khẩu mới
        String encodedPassword = passwordEncoder.encode(resetPasswordRequest.getNewPassword());
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);

        // Xóa record ForgotPassword sau khi reset thành công
        forgotPasswordRepository.deleteByUser(user);

        return ResponseEntity.ok("Đặt lại mật khẩu thành công!");
    }

    private Integer otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }
}
