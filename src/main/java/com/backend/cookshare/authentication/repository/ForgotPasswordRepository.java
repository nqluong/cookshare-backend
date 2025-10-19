package com.backend.cookshare.authentication.repository;

import com.backend.cookshare.authentication.entity.ForgotPassword;
import com.backend.cookshare.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword, UUID> {
    @Query("SELECT fp FROM ForgotPassword fp WHERE fp.otp = ?1 and fp.user = ?2")
    Optional<ForgotPassword> findByOtpAndUser(Integer otp, User user);

    @Query("SELECT fp FROM ForgotPassword fp WHERE fp.user = ?1 and fp.isVerified = ?2")
    Optional<ForgotPassword> findByUserAndIsVerified(User user, Boolean isVerified);

    @Transactional
    @Modifying
    @Query("DELETE FROM ForgotPassword fp WHERE fp.user = ?1")
    void deleteByUser(User user);
}
