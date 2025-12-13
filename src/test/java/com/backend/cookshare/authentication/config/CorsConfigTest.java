package com.backend.cookshare.authentication.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    private final CorsConfig corsConfig = new CorsConfig();

    @Test
    void corsConfigurationSource_ShouldReturnValidConfiguration() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        assertNotNull(source);

        // Dùng MockHttpServletRequest thay vì null
        MockHttpServletRequest request = new MockHttpServletRequest();
        CorsConfiguration configuration = source.getCorsConfiguration(request);
        assertNotNull(configuration);

        assertEquals(List.of("*"), configuration.getAllowedOriginPatterns());
        assertEquals(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"), configuration.getAllowedMethods());
        assertEquals(List.of("*"), configuration.getAllowedHeaders());
        assertEquals(List.of("Authorization", "Content-Type"), configuration.getExposedHeaders());
        assertTrue(configuration.getAllowCredentials());
    }
}
