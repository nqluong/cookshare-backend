package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.entity.ForgotPassword;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.ForgotPasswordRepository;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.EmailService;
import com.backend.cookshare.authentication.service.EmailVerificationService;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ForgotPasswordRepository forgotPasswordRepository;

    @Override
    public String sendVerificationOtp() {
        // Lấy user hiện tại từ SecurityContext
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra email đã được xác thực chưa
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            log.info("Email already verified for user: {}", user.getUsername());
            return "Email đã được xác thực trước đó";
        }

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
                    user.getEmail(),
                    "CookShare - Xác thực địa chỉ Email",
                    "email-verification-otp",
                    context
            );

            forgotPasswordRepository.save(fp);
            log.info("OTP sent to user {} at email {}", user.getUsername(), user.getEmail());
            return "Mã OTP đã được gửi đến email: " + maskEmail(user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send OTP email to user: {}", user.getUsername(), e);
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Override
    public String verifyOtp(Integer otp) {
        // Lấy user hiện tại từ SecurityContext
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra email đã được xác thực chưa
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            log.info("Email already verified for user: {}", user.getUsername());
            return "Email đã được xác thực trước đó";
        }

        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new CustomException(ErrorCode.OTP_NOT_FOUND));

        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteByUser(user);
            log.warn("OTP expired for user: {}", user.getUsername());
            throw new CustomException(ErrorCode.OTP_EXPIRED);
        }

        // Cập nhật trạng thái email verified
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Xóa OTP sau khi xác thực thành công
        forgotPasswordRepository.deleteByUser(user);

        log.info("Email verified successfully for user: {}", user.getUsername());
        return "Xác thực email thành công!";
    }

    /**
     * Generate OTP ngẫu nhiên 6 chữ số
     */
    private Integer generateOtp() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }

    /**
     * Ẩn bớt email để bảo mật
     */
    private String maskEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length != 2) return email;

        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***@" + domain;
        }

        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + domain;
    }
}

