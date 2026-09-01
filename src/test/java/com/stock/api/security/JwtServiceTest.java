package com.stock.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour JwtService.
 * Couvre : génération, extraction et validation de tokens JWT.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Configurer les propriétés JWT via ReflectionTestUtils
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "Z3VpeGJ1ZmYtc2VjcmV0LWtleS1mb3Itc3RvY2stYXBpLTIwMjYtdmVyeS1sb25nLXNlY3JldC1rZXk=");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L); // 24h
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 jours

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // Génération de tokens
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("generateToken() — Génération")
    class GenerateTokenTests {

        @Test
        @DisplayName("Génère un token non vide")
        void generateToken_notEmpty() {
            String token = jwtService.generateToken(userDetails);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Le token contient 3 parties séparées par des points")
        void generateToken_hasThreeParts() {
            String token = jwtService.generateToken(userDetails);

            String[] parts = token.split("\\.");
            assertEquals(3, parts.length, "Le JWT doit contenir 3 parties (header, payload, signature)");
        }

        @Test
        @DisplayName("Le token contient le bon subject (email)")
        void generateToken_containsSubject() {
            String token = jwtService.generateToken(userDetails);

            String extractedEmail = jwtService.extractEmail(token);
            assertEquals("test@example.com", extractedEmail);
        }

        @Test
        @DisplayName("Le token n'est pas expiré immédiatement")
        void generateToken_notExpired() {
            String token = jwtService.generateToken(userDetails);

            assertFalse(jwtService.extractExpiration(token).before(new Date()),
                    "Le token ne devrait pas être expiré immédiatement après génération");
        }
    }

    // ═══════════════════════════════════════════════════════
    // Extraction de claims
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("Extraction de claims")
    class ExtractClaimsTests {

        @Test
        @DisplayName("extractEmail() retourne le bon email")
        void extractEmail_correctValue() {
            String token = jwtService.generateToken(userDetails);

            String email = jwtService.extractEmail(token);

            assertEquals("test@example.com", email);
        }

        @Test
        @DisplayName("extractExpiration() retourne une date future")
        void extractExpiration_futureDate() {
            String token = jwtService.generateToken(userDetails);

            Date expiration = jwtService.extractExpiration(token);

            assertNotNull(expiration);
            assertTrue(expiration.after(new Date()),
                    "L'expiration devrait être dans le futur");
        }
    }

    // ═══════════════════════════════════════════════════════
    // Validation de tokens
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("isTokenValid() — Validation")
    class ValidateTokenTests {

        @Test
        @DisplayName("Token valide → true")
        void isTokenValid_validToken() {
            String token = jwtService.generateToken(userDetails);

            assertTrue(jwtService.isTokenValid(token, userDetails));
        }

        @Test
        @DisplayName("Token pour un autre utilisateur → false")
        void isTokenValid_wrongUser() {
            String token = jwtService.generateToken(userDetails);

            UserDetails otherUser = User.builder()
                    .username("other@example.com")
                    .password("password")
                    .authorities(Collections.emptyList())
                    .build();

            assertFalse(jwtService.isTokenValid(token, otherUser));
        }

        @Test
        @DisplayName("Token corrompu → exception")
        void isTokenValid_corruptedToken() {
            String corruptedToken = "eyJhbGciOiJIUzI1NiJ9.corrupted.payload";

            assertThrows(Exception.class,
                    () -> jwtService.isTokenValid(corruptedToken, userDetails));
        }
    }

    // ═══════════════════════════════════════════════════════
    // Refresh Token
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("generateRefreshToken() — Refresh Token")
    class RefreshTokenTests {

        @Test
        @DisplayName("Le refresh token a une expiration plus lointaine que le token normal")
        void refreshToken_longerExpiration() {
            String normalToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            Date normalExpiration = jwtService.extractExpiration(normalToken);
            Date refreshExpiration = jwtService.extractExpiration(refreshToken);

            assertTrue(refreshExpiration.after(normalExpiration),
                    "Le refresh token doit avoir une expiration plus lointaine");
        }

        @Test
        @DisplayName("Le refresh token est valide pour le bon utilisateur")
        void refreshToken_validForUser() {
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            assertTrue(jwtService.isTokenValid(refreshToken, userDetails));
        }
    }
}
