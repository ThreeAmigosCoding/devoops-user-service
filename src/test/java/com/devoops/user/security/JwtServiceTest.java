package com.devoops.user.security;

import com.devoops.user.entity.Role;
import com.devoops.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    // Base64 encoded secret key (at least 256 bits for HS256)
    private static final String SECRET_KEY = "dGhpcyBpcyBhIHZlcnkgc2VjdXJlIGtleSBmb3IgSFMyNTYgdGhhdCBpcyBhdCBsZWFzdCAyNTYgYml0cyBsb25n";
    private static final long EXPIRATION_TIME = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_TIME);

        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .password("encodedPassword")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .residence("Belgrade")
                .role(Role.GUEST)
                .build();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void generateToken_WithValidUser_ReturnsToken() {
        // When
        String token = jwtService.generateToken(user);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should extract username from token")
    void extractUsername_WithValidToken_ReturnsUsername() {
        // Given
        String token = jwtService.generateToken(user);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should validate token successfully")
    void isTokenValid_WithValidToken_ReturnsTrue() {
        // Given
        String token = jwtService.generateToken(user);

        // When
        boolean isValid = jwtService.isTokenValid(token, user);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false for token with different user")
    void isTokenValid_WithDifferentUser_ReturnsFalse() {
        // Given
        String token = jwtService.generateToken(user);
        User differentUser = User.builder()
                .id(UUID.randomUUID())
                .username("differentuser")
                .build();

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should throw exception for expired token")
    void isTokenExpired_WithExpiredToken_ThrowsExpiredJwtException() {
        // Given - Create a service with very short expiration
        JwtService shortExpiryService = new JwtService();
        ReflectionTestUtils.setField(shortExpiryService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(shortExpiryService, "jwtExpiration", -1000L); // Already expired

        String token = shortExpiryService.generateToken(user);

        // When/Then - Parsing an expired token throws ExpiredJwtException
        org.junit.jupiter.api.Assertions.assertThrows(
                io.jsonwebtoken.ExpiredJwtException.class,
                () -> shortExpiryService.isTokenExpired(token)
        );
    }

    @Test
    @DisplayName("Should return correct expiration time")
    void getExpirationTime_ReturnsConfiguredValue() {
        // When
        long expirationTime = jwtService.getExpirationTime();

        // Then
        assertThat(expirationTime).isEqualTo(EXPIRATION_TIME);
    }

    @Test
    @DisplayName("Should extract custom claims from token")
    void extractClaim_WithCustomClaims_ReturnsCorrectValues() {
        // Given
        String token = jwtService.generateToken(user);

        // When
        String userId = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
        String email = jwtService.extractClaim(token, claims -> claims.get("email", String.class));
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

        // Then
        assertThat(userId).isEqualTo(user.getId().toString());
        assertThat(email).isEqualTo("test@example.com");
        assertThat(role).isEqualTo("GUEST");
    }
}
