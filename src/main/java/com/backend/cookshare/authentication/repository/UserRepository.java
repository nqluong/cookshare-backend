package com.backend.cookshare.authentication.repository;

import com.backend.cookshare.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    //Tìm người dùng theo username.
    Optional<User> findByUsername(String username);
    User findByUserId(UUID userId);
    //Tìm người dùng theo email.
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByFacebookId(String facebookId);

    @Query("SELECT u FROM User u WHERE u.username = ?1 OR u.email = ?1")
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    //Kiểm tra xem có tồn tại người dùng với username đã cho không.
    boolean existsByUsername(String username);

    //Kiểm tra xem có tồn tại người dùng với email đã cho không.
    boolean existsByEmail(String email);

    User findByRefreshTokenAndUsername(String token, String username);
}