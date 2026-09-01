package com.stock.api.service;

import com.stock.api.dto.AuthResponse;
import com.stock.api.dto.LoginRequest;
import com.stock.api.dto.RegisterRequest;
import com.stock.api.entity.Role;
import com.stock.api.entity.User;
import com.stock.api.repository.UserRepository;
import com.stock.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service d'authentification : inscription et connexion.
 * US-01: Inscription d'un nouvel utilisateur
 * US-02: Connexion et récupération d'un JWT
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * US-01 : Inscription d'un nouvel utilisateur.
     * RG-05 : au moins un rôle actif.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Vérifier si l'email est déjà utilisé
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Un compte avec cet email existe déjà");
        }

        // RG-05 : au moins un rôle
        Set<Role> roles;
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            roles = request.getRoles().stream()
                    .map(roleName -> Role.valueOf(roleName.toUpperCase()))
                    .collect(Collectors.toSet());
        } else {
            // Rôle par défaut : USER
            roles = Set.of(Role.USER);
        }

        // Construire et sauvegarder l'utilisateur
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(roles)
                .active(true)
                .build();

        userRepository.save(user);

        // Générer le token JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(user, token);
    }

    /**
     * US-02 : Connexion et récupération d'un JWT.
     */
    public AuthResponse login(LoginRequest request) {
        // Authentification via Spring Security AuthenticationManager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Charger l'utilisateur et générer le token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream()
                        .map(Role::name)
                        .collect(Collectors.toSet()))
                .build();
    }
}
