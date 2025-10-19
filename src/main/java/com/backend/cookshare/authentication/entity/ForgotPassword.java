package com.backend.cookshare.authentication.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "forgot_passwords")
public class ForgotPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "fp_id")
    UUID fpid;

    @Column(name = "otp", nullable = false, length = 6)
    Integer otp;

    @Column(name = "expiration_time", nullable = false)
    Date expirationTime;

    @Column(name = "is_verified")
    @Builder.Default
    Boolean isVerified = false;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
