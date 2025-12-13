package com.backend.cookshare.authentication.config;

import com.backend.cookshare.authentication.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtBlacklistFilterTest {

    private JwtBlacklistFilter filter;
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = mock(TokenBlacklistService.class);
        filter = new JwtBlacklistFilter(tokenBlacklistService);
    }

    @Test
    void doFilterInternal_TokenNotBlacklisted_ShouldCallNextFilter() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/some-path");
        when(request.getHeader("Authorization")).thenReturn("Bearer validToken");
        when(tokenBlacklistService.isTokenBlacklisted("validToken")).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_TokenBlacklisted_ShouldReturnUnauthorized() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getRequestURI()).thenReturn("/some-path");
        when(request.getHeader("Authorization")).thenReturn("Bearer blacklistedToken");
        when(tokenBlacklistService.isTokenBlacklisted("blacklistedToken")).thenReturn(true);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        writer.flush(); // flush the writer to populate stringWriter

        String responseBody = stringWriter.toString();
        assertTrue(responseBody.contains("Access token không hợp lệ"));
        assertTrue(responseBody.contains("4004"));

        // Filter chain should NOT be called
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void shouldNotFilter_ExcludedPaths_ShouldReturnTrue() {
        assertTrue(filter.shouldNotFilter(mockRequest("/ws")));
        assertTrue(filter.shouldNotFilter(mockRequest("/ws-sockjs")));
        assertTrue(filter.shouldNotFilter(mockRequest("/auth/login")));
        assertTrue(filter.shouldNotFilter(mockRequest("/auth/register")));
        assertTrue(filter.shouldNotFilter(mockRequest("/auth/refresh")));
    }

    @Test
    void shouldNotFilter_OtherPaths_ShouldReturnFalse() {
        assertFalse(filter.shouldNotFilter(mockRequest("/other")));
        assertFalse(filter.shouldNotFilter(mockRequest("/api/test")));
    }

    private HttpServletRequest mockRequest(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}
