package com.backend.cookshare.authentication.config;

import com.backend.cookshare.authentication.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomPermissionEvaluatorTest {

    @InjectMocks
    private CustomPermissionEvaluator permissionEvaluator;

    @Mock
    private Authentication authentication;

    private Jwt createJwt(String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("sub", "test-user");

        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                claims
        );
    }

    @Test
    void hasPermission_NullAuthentication_ShouldReturnFalse() {
        boolean result = permissionEvaluator.hasPermission(null, null, "USER");
        assertFalse(result);
    }

    @Test
    void hasPermission_NotAuthenticated_ShouldReturnFalse() {
        when(authentication.isAuthenticated()).thenReturn(false);
        boolean result = permissionEvaluator.hasPermission(authentication, null, "USER");
        assertFalse(result);
    }

    @Test
    void hasPermission_AdminRole_ShouldAlwaysReturnTrue() {
        when(authentication.isAuthenticated()).thenReturn(true);
        Jwt jwt = createJwt(UserRole.Values.ADMIN);
        when(authentication.getPrincipal()).thenReturn(jwt);

        assertTrue(permissionEvaluator.hasPermission(authentication, null, "USER"));
        assertTrue(permissionEvaluator.hasPermission(authentication, null, "ADMIN"));
    }

    @Test
    void hasPermission_UserRoleWithJwt_ShouldCheckCorrectly() {
        when(authentication.isAuthenticated()).thenReturn(true);
        Jwt jwt = createJwt(UserRole.Values.USER);
        when(authentication.getPrincipal()).thenReturn(jwt);

        assertTrue(permissionEvaluator.hasPermission(authentication, null, "USER"));
        assertFalse(permissionEvaluator.hasPermission(authentication, null, "ADMIN"));
    }

    @Test
    void hasPermission_FallbackToAuthorities_ShouldWork() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("username"); // Not JWT

        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .when(authentication).getAuthorities();

        assertTrue(permissionEvaluator.hasPermission(authentication, null, "USER"));
        assertFalse(permissionEvaluator.hasPermission(authentication, null, "ADMIN"));
    }

    @Test
    void hasPermission_WithTargetId_ShouldDelegateToMainMethod() {
        when(authentication.isAuthenticated()).thenReturn(true);
        Jwt jwt = createJwt(UserRole.Values.ADMIN);
        when(authentication.getPrincipal()).thenReturn(jwt);

        boolean result = permissionEvaluator.hasPermission(authentication, 123L, "Recipe", "USER");
        assertTrue(result);
    }
}