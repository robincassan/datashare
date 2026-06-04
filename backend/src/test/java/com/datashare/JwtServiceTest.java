package com.datashare;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("my-test-secret-key-that-is-long-enough-for-hmac!", 3600000);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        String token = jwtService.generateToken("user-123");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractUserId_shouldReturnCorrectUserId() {
        String token = jwtService.generateToken("user-123");
        String userId = jwtService.extractUserId(token);
        assertEquals("user-123", userId);
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        String token = jwtService.generateToken("user-123");
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid-token"));
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() throws Exception {
        JwtService shortLived = new JwtService("my-test-secret-key-that-is-long-enough-for-hmac!", 1);
        String token = shortLived.generateToken("user-123");
        Thread.sleep(10);
        assertFalse(shortLived.isTokenValid(token));
    }

    @Test
    void extractUserId_shouldThrowForInvalidToken() {
        assertThrows(JwtException.class, () -> jwtService.extractUserId("invalid-token"));
    }
}
