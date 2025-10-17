package com.backend.cookshare.authentication.config;

import com.backend.cookshare.authentication.enums.UserRole;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Kiểm tra nếu user có role ADMIN thì luôn có quyền
        if (hasRole(authentication, UserRole.Values.ADMIN)) {
            return true;
        }

        // Kiểm tra quyền cụ thể
        String requiredRole = permission.toString();
        return hasRole(authentication, requiredRole);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String userRole = jwt.getClaimAsString("role");
            return role.equals(userRole);
        }

        // Fallback to authorities
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + role));
    }
}
