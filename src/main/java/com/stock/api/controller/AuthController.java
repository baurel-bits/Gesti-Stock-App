package com.stock.api.controller;

import com.stock.api.dto.AuthResponse;
import com.stock.api.dto.LoginRequest;
import com.stock.api.dto.RegisterRequest;
import com.stock.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * US-01 : Inscription d'un nouvel utilisateur
 * US-02 : Connexion et récupération d'un JWT
 * US-03 : Gestion des erreurs d'authentification
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription et connexion des utilisateurs")
public class AuthController {

    private final AuthService authService;

    /**
     * US-01 : Inscription d'un nouvel utilisateur.
     */
    @PostMapping("/register")
    @Operation(summary = "Inscrire un nouvel utilisateur",
               description = "Crée un nouveau compte utilisateur avec au moins un rôle (RG-05)")
    @ApiResponse(responseCode = "201", description = "Utilisateur inscrit avec succès")
    @ApiResponse(responseCode = "400", description = "Données invalides ou email déjà utilisé")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * US-02 : Connexion et récupération d'un JWT.
     */
    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur",
               description = "Authentifie l'utilisateur et retourne un JWT valide")
    @ApiResponse(responseCode = "200", description = "Connexion réussie, JWT retourné")
    @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
