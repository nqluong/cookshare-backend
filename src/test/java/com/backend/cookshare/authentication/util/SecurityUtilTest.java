package com.backend.cookshare.authentication.util;

import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.enums.UserRole;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    private SecurityUtil securityUtil;
    private String base64Key;
    private byte[] keyBytes;

    @BeforeEach
    void setUp() throws Exception {
        // HS512 cần ít nhất 512 bits (64 bytes)
        // Tạo key đủ dài bằng cách hash một string dài
        String longSecret = "this-is-a-very-long-secret-key-for-HS512-that-is-long-enough-to-meet-the-minimum-requirement";
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        keyBytes = digest.digest(longSecret.getBytes(StandardCharsets.UTF_8));
        base64Key = java.util.Base64.getEncoder().encodeToString(keyBytes);

        SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityUtil.JWT_ALGORITHM.getName());
        JwtEncoder realEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));

        securityUtil = new SecurityUtil(realEncoder);

        // Set private fields
        setPrivateField(securityUtil, "jwtKey", base64Key);
        setPrivateField(securityUtil, "accessTokenExpiration", 3600L);
        setPrivateField(securityUtil, "refreshTokenExpiration", 7200L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private String createExpiredToken(String username) throws Exception {
        // Manually create an expired JWT using Nimbus
        JWSSigner signer = new MACSigner(keyBytes);

        Instant now = Instant.now();
        Instant expiry = now.minus(3600, ChronoUnit.SECONDS); // Expired 1 hour ago

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(username)
                .issueTime(Date.from(now.minus(7200, ChronoUnit.SECONDS)))
                .expirationTime(Date.from(expiry))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS512),
                claimsSet
        );

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    @Test
    void createAccessToken_ShouldReturnValidToken() {
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .username("testuser")
                .role(UserRole.USER)
                .build();

        String token = securityUtil.createAccessToken("testuser", userInfo);
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // JWT format: header.payload.signature

        // Decode lại token để kiểm tra
        Jwt decoded = securityUtil.checkValidAccessToken(token);
        assertEquals("testuser", decoded.getSubject());
        assertEquals("USER", decoded.getClaim("role"));
        assertNotNull(decoded.getClaim("user"));
    }

    @Test
    void createAccessToken_WithAdminRole_ShouldIncludeAdminRole() {
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .username("adminuser")
                .role(UserRole.ADMIN)
                .build();

        String token = securityUtil.createAccessToken("adminuser", userInfo);
        Jwt decoded = securityUtil.checkValidAccessToken(token);

        assertEquals("adminuser", decoded.getSubject());
        assertEquals("ADMIN", decoded.getClaim("role"));
    }

    @Test
    void createRefreshToken_ShouldReturnValidToken() {
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .username("testuser")
                .role(UserRole.USER)
                .build();

        LoginResponseDTO dto = LoginResponseDTO.builder()
                .user(userInfo)
                .build();

        String token = securityUtil.createRefreshToken("testuser", dto);
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        Jwt decoded = securityUtil.checkValidRefreshToken(token);
        assertEquals("testuser", decoded.getSubject());
        assertNotNull(decoded.getClaim("user"));
    }

    @Test
    void checkValidAccessToken_WithValidToken_ShouldDecodeSuccessfully() {
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .username("testuser")
                .role(UserRole.USER)
                .build();

        String token = securityUtil.createAccessToken("testuser", userInfo);

        Jwt decoded = securityUtil.checkValidAccessToken(token);
        assertNotNull(decoded);
        assertEquals("testuser", decoded.getSubject());
    }

    @Test
    void checkValidAccessToken_WithInvalidToken_ShouldThrowException() {
        String invalidToken = "invalid.token.here";

        assertThrows(JwtException.class, () -> {
            securityUtil.checkValidAccessToken(invalidToken);
        });
    }

    @Test
    void checkValidAccessToken_WithMalformedToken_ShouldThrowException() {
        String malformedToken = "not-a-valid-jwt";

        assertThrows(JwtException.class, () -> {
            securityUtil.checkValidAccessToken(malformedToken);
        });
    }

    @Test
    void checkValidAccessToken_WithExpiredToken_ShouldThrowException() throws Exception {
        String expiredToken = createExpiredToken("testuser");

        assertThrows(JwtException.class, () -> {
            securityUtil.checkValidAccessToken(expiredToken);
        });
    }

    @Test
    void checkValidRefreshToken_WithValidToken_ShouldDecodeSuccessfully() {
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .username("testuser")
                .role(UserRole.USER)
                .build();

        LoginResponseDTO dto = LoginResponseDTO.builder()
                .user(userInfo)
                .build();

        String token = securityUtil.createRefreshToken("testuser", dto);

        Jwt decoded = securityUtil.checkValidRefreshToken(token);
        assertNotNull(decoded);
        assertEquals("testuser", decoded.getSubject());
    }

    @Test
    void checkValidRefreshToken_WithInvalidToken_ShouldThrowException() {
        String invalidToken = "invalid.refresh.token";

        assertThrows(JwtException.class, () -> {
            securityUtil.checkValidRefreshToken(invalidToken);
        });
    }

    @Test
    void checkValidRefreshToken_WithExpiredToken_ShouldThrowException() throws Exception {
        String expiredToken = createExpiredToken("testuser");

        assertThrows(JwtException.class, () -> {
            securityUtil.checkValidRefreshToken(expiredToken);
        });
    }

    @Test
    void getCurrentUserLogin_WithJwtAuthentication_ShouldReturnUsername() {
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .username("currentuser")
                .role(UserRole.USER)
                .build();

        String token = securityUtil.createAccessToken("currentuser", userInfo);
        Jwt jwt = securityUtil.checkValidAccessToken(token);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        Optional<String> currentUser = SecurityUtil.getCurrentUserLogin();
        assertTrue(currentUser.isPresent());
        assertEquals("currentuser", currentUser.get());
    }

    @Test
    void getCurrentUserLogin_WithUserDetailsAuthentication_ShouldReturnUsername() {
        UserDetails userDetails = User.builder()
                .username("userdetailsuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> currentUser = SecurityUtil.getCurrentUserLogin();
        assertTrue(currentUser.isPresent());
        assertEquals("userdetailsuser", currentUser.get());
    }

    @Test
    void getCurrentUserLogin_WithStringPrincipal_ShouldReturnUsername() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "stringuser", null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> currentUser = SecurityUtil.getCurrentUserLogin();
        assertTrue(currentUser.isPresent());
        assertEquals("stringuser", currentUser.get());
    }

    @Test
    void getCurrentUserLogin_WithNoAuthentication_ShouldReturnEmpty() {
        SecurityContextHolder.clearContext();

        Optional<String> currentUser = SecurityUtil.getCurrentUserLogin();
        assertFalse(currentUser.isPresent());
    }

    @Test
    void getCurrentUserLogin_WithNullAuthentication_ShouldReturnEmpty() {
        SecurityContextHolder.getContext().setAuthentication(null);

        Optional<String> currentUser = SecurityUtil.getCurrentUserLogin();
        assertFalse(currentUser.isPresent());
    }

    @Test
    void getCurrentUserLogin_WithUnknownPrincipalType_ShouldReturnEmpty() {
        // Principal là object khác không phải UserDetails, Jwt, hay String
        Object unknownPrincipal = new Object();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                unknownPrincipal, null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> currentUser = SecurityUtil.getCurrentUserLogin();
        assertFalse(currentUser.isPresent());
    }

    @Test
    void tokenExpiration_RefreshTokenShouldBeLongerThanAccessToken() throws Exception {
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .username("testuser")
                .role(UserRole.USER)
                .build();

        LoginResponseDTO dto = LoginResponseDTO.builder()
                .user(userInfo)
                .build();

        String accessToken = securityUtil.createAccessToken("testuser", userInfo);
        String refreshToken = securityUtil.createRefreshToken("testuser", dto);

        Jwt accessDecoded = securityUtil.checkValidAccessToken(accessToken);
        Jwt refreshDecoded = securityUtil.checkValidRefreshToken(refreshToken);

        // Refresh token phải có thời gian hết hạn lâu hơn access token
        assertTrue(refreshDecoded.getExpiresAt().isAfter(accessDecoded.getExpiresAt()));
    }

    @Test
    void createAccessToken_WithCompleteUserInfo_ShouldIncludeAllClaims() {
        LoginResponseDTO.UserInfo userInfo = LoginResponseDTO.UserInfo.builder()
                .userId(UUID.randomUUID())
                .username("fulluser")
                .email("test@example.com")
                .fullName("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .bio("Test bio")
                .role(UserRole.USER)
                .isActive(true)
                .emailVerified(true)
                .followingCount(10)
                .followerCount(20)
                .recipeCount(5)
                .build();

        String token = securityUtil.createAccessToken("fulluser", userInfo);
        Jwt decoded = securityUtil.checkValidAccessToken(token);

        assertEquals("fulluser", decoded.getSubject());
        assertEquals("USER", decoded.getClaim("role"));
        assertNotNull(decoded.getClaim("user"));
        assertNotNull(decoded.getIssuedAt());
        assertNotNull(decoded.getExpiresAt());
    }

    @Test
    void jwtAlgorithm_ShouldBeHS512() {
        assertEquals("HS512", SecurityUtil.JWT_ALGORITHM.getName());
    }
}