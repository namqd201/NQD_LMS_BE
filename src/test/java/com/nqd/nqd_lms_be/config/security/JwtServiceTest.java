package com.nqd.nqd_lms_be.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "test-super-secret-key-that-is-longer-than-256-bits-for-testing-purposes-123456";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, 7);
    }

    @Test
    @DisplayName("Should successfully generate and validate 7-day JWT token")
    void generateAndValidateToken_success() {
        UUID userId = UUID.randomUUID();
        String email = "test@nqdlms.com";
        String fullName = "Quách Duy Nam";
        Set<String> roles = Set.of("STUDENT", "TEACHER");

        String token = jwtService.generateToken(userId, email, fullName, roles);
        assertNotNull(token);
        assertFalse(token.isBlank());

        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.getUserIdFromToken(token));
        assertEquals(email, jwtService.getEmailFromToken(token));
        assertEquals(roles, jwtService.getRolesFromToken(token));
    }

    @Test
    @DisplayName("Should return false for tampered or invalid token")
    void validateToken_tamperedToken_returnsFalse() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "test@nqdlms.com", "Test User", Set.of("STUDENT"));

        // Tamper with the token string
        String tamperedToken = token + "xyz";
        assertFalse(jwtService.validateToken(tamperedToken));

        // Invalid token formats
        assertFalse(jwtService.validateToken(""));
        assertFalse(jwtService.validateToken(null));
        assertFalse(jwtService.validateToken("not.a.jwt"));
    }

    @Test
    @DisplayName("Should return false for expired token")
    void validateToken_expiredToken_returnsFalse() {
        // Create a JwtService with 0 or negative days
        JwtService expiredJwtService = new JwtService(secret, -1);
        UUID userId = UUID.randomUUID();
        String token = expiredJwtService.generateToken(userId, "expired@nqdlms.com", "Expired User", Set.of("STUDENT"));

        assertFalse(jwtService.validateToken(token));
    }
}
