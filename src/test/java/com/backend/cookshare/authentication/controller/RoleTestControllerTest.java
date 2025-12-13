package com.backend.cookshare.authentication.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoleTestControllerTest {

    private RoleTestController controller;

    @BeforeEach
    void setUp() {
        controller = new RoleTestController();
    }

    // ===================== USER ROLE =====================
    @Test
    void userEndpoint_WithUserRole_ShouldReturnOk() {
        // Mock authentication USER
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password",
                        List.of(() -> "USER"))
        );

        ResponseEntity<String> response = controller.userEndpoint();

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("USER"));
    }

    @Test
    void anyRoleEndpoint_WithUserRole_ShouldReturnOk() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password",
                        List.of(() -> "USER"))
        );

        ResponseEntity<String> response = controller.anyRoleEndpoint();

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("USER"));
    }

    // ===================== ADMIN ROLE =====================
    @Test
    void adminEndpoint_WithAdminRole_ShouldReturnOk() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "password",
                        List.of(() -> "ADMIN"))
        );

        ResponseEntity<String> response = controller.adminEndpoint();

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("ADMIN"));
    }

    @Test
    void adminEndpoint1_WithAdminRole_ShouldReturnOk() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "password",
                        List.of(() -> "ADMIN"))
        );

        ResponseEntity<String> response = controller.adminEndpoint1();

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("ADMIN"));
    }
}
