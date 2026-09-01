package com.stock.api.service;

import com.stock.api.dto.AuthResponse;
import com.stock.api.dto.LoginRequest;
import com.stock.api.dto.RegisterRequest;
import com.stock.api.entity.Role;
import com.stock.api.entity.User;
import com.stock.api.repository.UserRepository;
import com.stock.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AuthService.
 * Couvre : US-01 (inscription), US-02 (connexion).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("Jean")
                .lastName("Dupont")
                .build();

        validLoginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("$2a$10$encoded-password")
                .firstName("Jean")
                .lastName("Dupont")
                .roles(Set.of(Role.USER))
                .active(true)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // US-01 : Inscription d'un nouvel utilisateur
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("register() — Inscription")
    class RegisterTests {

        @Test
        @DisplayName("Inscription réussie avec rôle par défaut USER")
        void register_success_defaultRole() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("test@example.com")
                    .password("$2a$10$encoded-password")
                    .authorities("ROLE_USER")
                    .build();
            when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("jwt-token-123");

            AuthResponse response = authService.register(validRegisterRequest);

            assertNotNull(response);
            assertEquals("jwt-token-123", response.getToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals("test@example.com", response.getEmail());
            assertEquals("Jean", response.getFirstName());
            assertEquals("Dupont", response.getLastName());
            assertTrue(response.getRoles().contains("USER"));

            verify(userRepository).existsByEmail("test@example.com");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Inscription avec rôle personnalisé ADMIN")
        void register_success_customRole() {
            RegisterRequest adminRequest = RegisterRequest.builder()
                    .email("admin@example.com")
                    .password("password123")
                    .firstName("Admin")
                    .lastName("Test")
                    .roles(Set.of("ADMIN"))
                    .build();

            User adminUser = User.builder()
                    .id(2L)
                    .email("admin@example.com")
                    .password("$2a$10$encoded-password")
                    .firstName("Admin")
                    .lastName("Test")
                    .roles(Set.of(Role.ADMIN))
                    .active(true)
                    .build();

            when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(adminUser);

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("admin@example.com")
                    .password("$2a$10$encoded-password")
                    .authorities("ROLE_ADMIN")
                    .build();
            when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("admin-jwt-token");

            AuthResponse response = authService.register(adminRequest);

            assertNotNull(response);
            assertTrue(response.getRoles().contains("ADMIN"));
            verify(userRepository).save(argThat(user -> user.getRoles().contains(Role.ADMIN)));
        }

        @Test
        @DisplayName("Email déjà utilisé → IllegalStateException")
        void register_duplicateEmail() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> authService.register(validRegisterRequest));

            assertEquals("Un compte avec cet email existe déjà", exception.getMessage());
            verify(userRepository).existsByEmail("test@example.com");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Rôle invalide → IllegalArgumentException")
        void register_invalidRole() {
            RegisterRequest invalidRoleRequest = RegisterRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .firstName("Jean")
                    .lastName("Dupont")
                    .roles(Set.of("INVALID_ROLE"))
                    .build();

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

            assertThrows(IllegalArgumentException.class,
                    () -> authService.register(invalidRoleRequest));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Le mot de passe est bien encodé avant sauvegarde")
        void register_passwordEncoded() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("test@example.com")
                    .password("$2a$10$hashed-password")
                    .authorities("ROLE_USER")
                    .build();
            when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("token");

            authService.register(validRegisterRequest);

            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(argThat(user ->
                    user.getPassword().equals("$2a$10$hashed-password")));
        }
    }

    // ═══════════════════════════════════════════════════════
    // US-02 : Connexion et récupération d'un JWT
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("login() — Connexion")
    class LoginTests {

        @Test
        @DisplayName("Connexion réussie → JWT retourné")
        void login_success() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("test@example.com")
                    .password("$2a$10$encoded-password")
                    .authorities("ROLE_USER")
                    .build();
            when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("jwt-token-456");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(savedUser));

            AuthResponse response = authService.login(validLoginRequest);

            assertNotNull(response);
            assertEquals("jwt-token-456", response.getToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals("test@example.com", response.getEmail());

            verify(authenticationManager).authenticate(any());
            verify(userDetailsService).loadUserByUsername("test@example.com");
            verify(jwtService).generateToken(userDetails);
        }

        @Test
        @DisplayName("Mauvais mot de passe → BadCredentialsException")
        void login_wrongPassword() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Email ou mot de passe incorrect"));

            assertThrows(BadCredentialsException.class,
                    () -> authService.login(validLoginRequest));

            verify(authenticationManager).authenticate(any());
            verify(userDetailsService, never()).loadUserByUsername(any());
            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Email inexistant → BadCredentialsException")
        void login_unknownEmail() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Email ou mot de passe incorrect"));

            LoginRequest unknownRequest = LoginRequest.builder()
                    .email("unknown@example.com")
                    .password("password123")
                    .build();

            assertThrows(BadCredentialsException.class,
                    () -> authService.login(unknownRequest));

            verify(authenticationManager).authenticate(any());
        }

        @Test
        @DisplayName("L'utilisateur est bien chargé par email")
        void login_loadsUserByEmail() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("test@example.com")
                    .password("$2a$10$encoded-password")
                    .authorities("ROLE_USER")
                    .build();
            when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("token");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(savedUser));

            authService.login(validLoginRequest);

            verify(userDetailsService).loadUserByUsername("test@example.com");
            verify(userRepository).findByEmail("test@example.com");
        }
    }

    // ═══════════════════════════════════════════════════════
    // US-03 : Gestion des erreurs d'authentification
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("Gestion des erreurs")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Le token JWT est bien généré après inscription")
        void register_generatesJwtToken() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("test@example.com")
                    .password("$2a$10$encoded-password")
                    .authorities("ROLE_USER")
                    .build();
            when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("generated-token");

            AuthResponse response = authService.register(validRegisterRequest);

            assertEquals("generated-token", response.getToken());
            verify(jwtService).generateToken(userDetails);
        }

        @Test
        @DisplayName("Le token JWT est bien généré après connexion")
        void login_generatesJwtToken() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("test@example.com")
                    .password("$2a$10$encoded-password")
                    .authorities("ROLE_USER")
                    .build();
            when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("login-token");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(savedUser));

            AuthResponse response = authService.login(validLoginRequest);

            assertEquals("login-token", response.getToken());
            verify(jwtService).generateToken(userDetails);
        }

        @Test
        @DisplayName("Les rôles de l'utilisateur sont bien dans la réponse")
        void register_rolesInResponse() {
            User multiRoleUser = User.builder()
                    .id(3L)
                    .email("multi@example.com")
                    .password("$2a$10$encoded")
                    .firstName("Multi")
                    .lastName("Role")
                    .roles(Set.of(Role.USER, Role.MANAGER))
                    .active(true)
                    .build();

            RegisterRequest multiRoleRequest = RegisterRequest.builder()
                    .email("multi@example.com")
                    .password("password123")
                    .firstName("Multi")
                    .lastName("Role")
                    .roles(Set.of("USER", "MANAGER"))
                    .build();

            when(userRepository.existsByEmail("multi@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(User.class))).thenReturn(multiRoleUser);

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("multi@example.com")
                    .password("$2a$10$encoded")
                    .authorities("ROLE_USER", "ROLE_MANAGER")
                    .build();
            when(userDetailsService.loadUserByUsername("multi@example.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("token");

            AuthResponse response = authService.register(multiRoleRequest);

            assertTrue(response.getRoles().contains("USER"));
            assertTrue(response.getRoles().contains("MANAGER"));
            assertEquals(2, response.getRoles().size());
        }
    }
}
