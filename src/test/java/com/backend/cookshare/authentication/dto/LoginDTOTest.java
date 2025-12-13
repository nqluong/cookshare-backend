package com.backend.cookshare.authentication.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoginDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testGetterSetter() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("user123");
        dto.setPassword("pass123");

        assertEquals("user123", dto.getUsername());
        assertEquals("pass123", dto.getPassword());
    }

    @Test
    void testValidation_ShouldFail_WhenUsernameBlank() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("");
        dto.setPassword("validPass");

        Set<ConstraintViolation<LoginDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("Username không được để trống")));
    }

    @Test
    void testValidation_ShouldFail_WhenPasswordBlank() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("validUser");
        dto.setPassword("");

        Set<ConstraintViolation<LoginDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("Password không được để trống")));
    }

    @Test
    void testValidation_ShouldPassWithValidData() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("validUser");
        dto.setPassword("validPass");

        Set<ConstraintViolation<LoginDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }
}
