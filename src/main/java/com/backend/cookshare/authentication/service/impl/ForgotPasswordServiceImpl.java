package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.request.ResetPasswordRequest;
import com.backend.cookshare.authentication.entity.ForgotPassword;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.ForgotPasswordRepository;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.EmailService;
import com.backend.cookshare.authentication.service.ForgotPasswordService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.Date;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String sendOtpForPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_EMAIL));

        Integer otp = generateOtp();

        // Xóa tất cả OTP cũ của user trước khi tạo mới
        forgotPasswordRepository.deleteByUser(user);

        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 60 * 1000 * 5)) // 5 phút
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
            log.info("Password reset OTP sent to email: {}", email);
            return "Xác minh email đã được gửi tới: " + email;
        } catch (Exception e) {
            log.error("Failed to send password reset OTP to email: {}", email, e);
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Override
    public String verifyOtpForPasswordReset(String email, Integer otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_EMAIL));

        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new CustomException(ErrorCode.OTP_NOT_FOUND));

        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteByUser(user);
            log.warn("OTP expired for email: {}", email);
            throw new CustomException(ErrorCode.OTP_EXPIRED);
        }

        // Đánh dấu OTP đã được xác thực
        fp.setIsVerified(true);
        forgotPasswordRepository.save(fp);

        log.info("OTP verified successfully for email: {}", email);
        return "OTP hợp lệ! Bạn có thể đặt lại mật khẩu.";
    }

    @Override
    public String resetPassword(String email, ResetPasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_EMAIL));

        // Kiểm tra xem OTP đã được xác thực chưa
        ForgotPassword fp = forgotPasswordRepository.findByUserAndIsVerified(user, true)
                .orElseThrow(() -> new CustomException(ErrorCode.OTP_NOT_VERIFIED));

        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteByUser(user);
            log.warn("OTP expired for email: {}", email);
            throw new CustomException(ErrorCode.OTP_EXPIRED);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // Mã hóa và cập nhật mật khẩu mới
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);

        // Xóa record ForgotPassword sau khi reset thành công
        forgotPasswordRepository.deleteByUser(user);

        log.info("Password reset successfully for email: {}", email);
        return "Đặt lại mật khẩu thành công!";
    }

    /**
     * Generate OTP ngẫu nhiên 6 chữ số
     */
    private Integer generateOtp() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }
}

