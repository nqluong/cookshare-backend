package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.LoginDTO;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.UserService;
import jakarta.validation.Valid;
import com.backend.cookshare.authentication.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;

    @Value("${cookshare.jwt.token-validity-in-seconds}")
    private long jwtExpiration;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRequest user) {
        String response = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDto) {
        //Nạp input gồm username/password vào Security
        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

        //xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        //create a token
        String access_token = this.securityUtil.createToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Lấy thông tin user từ database
        User user = userService.getUserByUsernameOrEmail(loginDto.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Cập nhật last active
        user.setLastActive(LocalDateTime.now());
        userService.updateUser(user);

        // Tạo response với thông tin chi tiết
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .lastActive(user.getLastActive())
                .followerCount(user.getFollowerCount())
                .followingCount(user.getFollowingCount())
                .recipeCount(user.getRecipeCount())
                .createdAt(user.getCreatedAt())
                .build();

        LoginResponseDTO response = LoginResponseDTO.builder()
                .accessToken(access_token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(userInfo)
                .build();

        return ResponseEntity.ok(response);
    }
}
