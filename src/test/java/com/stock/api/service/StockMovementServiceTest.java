package com.stock.api.service;

import com.stock.api.dto.StockMovementRequest;
import com.stock.api.dto.StockMovementResponse;
import com.stock.api.entity.Product;
import com.stock.api.entity.StockMovement;
import com.stock.api.entity.User;
import com.stock.api.repository.ProductRepository;
import com.stock.api.repository.StockMovementRepository;
import com.stock.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour StockMovementService.
 * RG-01 : quantité jamais négative.
 * RG-02 : rejet des sorties si quantité insuffisante.
 */
@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StockMovementService stockMovementService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .build();

        product = Product.builder()
                .id(1L)
                .name("Produit Test")
                .reference("REF-001")
                .quantity(10)
                .alertThreshold(5)
                .price(BigDecimal.valueOf(29.99))
                .deleted(false)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // RG-02 : Rejet des sorties si quantité insuffisante
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("RG-02 — Sortie rejetée si stock insuffisant")
    class InsufficientStockTests {

        @Test
        @DisplayName("Sortie avec stock insuffisant → IllegalStateException")
        void exit_insufficientStock_throwsException() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

            StockMovementRequest request = StockMovementRequest.builder()
                    .type(StockMovement.MovementType.EXIT)
                    .productId(1L)
                    .quantity(15) // Plus que les 10 disponibles
                    .reason("Test sortie")
                    .build();

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> stockMovementService.create(request, "user@test.com"));

            assertTrue(exception.getMessage().contains("Quantité insuffisante"));
            assertTrue(exception.getMessage().contains("Disponible: 10"));
            assertTrue(exception.getMessage().contains("Demandé: 15"));

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Sortie exactement égale au stock → autorisée")
        void exit_exactStockAllowed() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(stockMovementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockMovementRequest request = StockMovementRequest.builder()
                    .type(StockMovement.MovementType.EXIT)
                    .productId(1L)
                    .quantity(10) // Exactement le stock disponible
                    .reason("Test sortie exacte")
                    .build();

            StockMovementResponse response = stockMovementService.create(request, "user@test.com");

            assertNotNull(response);
            assertEquals(StockMovement.MovementType.EXIT, response.getType());
        }

        @Test
        @DisplayName("Sortie depuis stock vide → rejetée")
        void exit_emptyStock_throwsException() {
            product.setQuantity(0);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

            StockMovementRequest request = StockMovementRequest.builder()
                    .type(StockMovement.MovementType.EXIT)
                    .productId(1L)
                    .quantity(1)
                    .reason("Test stock vide")
                    .build();

            assertThrows(IllegalStateException.class,
                    () -> stockMovementService.create(request, "user@test.com"));
        }
    }

    // ═══════════════════════════════════════════════════════
    // RG-01 : Mise à jour de la quantité
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("RG-01 — Mise à jour de la quantité du produit")
    class QuantityUpdateTests {

        @Test
        @DisplayName("Entrée de stock → quantité augmentée")
        void entry_increasesQuantity() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(stockMovementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockMovementRequest request = StockMovementRequest.builder()
                    .type(StockMovement.MovementType.ENTRY)
                    .productId(1L)
                    .quantity(5)
                    .reason("Réapprovisionnement")
                    .build();

            stockMovementService.create(request, "user@test.com");

            assertEquals(15, product.getQuantity(),
                    "RG-01 : l'entrée doit augmenter la quantité");
        }

        @Test
        @DisplayName("Sortie de stock → quantité diminuée mais jamais négative")
        void exit_decreasesQuantityNeverNegative() {
            product.setQuantity(3);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(stockMovementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockMovementRequest request = StockMovementRequest.builder()
                    .type(StockMovement.MovementType.EXIT)
                    .productId(1L)
                    .quantity(3)
                    .reason("Sortie partielle")
                    .build();

            stockMovementService.create(request, "user@test.com");

            assertEquals(0, product.getQuantity(),
                    "RG-01 : la quantité ne doit jamais devenir négative");
        }
    }

    // ═══════════════════════════════════════════════════════
    // Cas d'erreur
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cas d'erreur")
    class ErrorCasesTests {

        @Test
        @DisplayName("Produit inexistant → IllegalArgumentException")
        void productNotFound() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            StockMovementRequest request = StockMovementRequest.builder()
                    .type(StockMovement.MovementType.ENTRY)
                    .productId(999L)
                    .quantity(5)
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> stockMovementService.create(request, "user@test.com"));
        }

        @Test
        @DisplayName("Produit supprimé → IllegalArgumentException")
        void deletedProduct() {
            product.setDeleted(true);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            StockMovementRequest request = StockMovementRequest.builder()
                    .type(StockMovement.MovementType.ENTRY)
                    .productId(1L)
                    .quantity(5)
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> stockMovementService.create(request, "user@test.com"));
        }

        @Test
        @DisplayName("Utilisateur inexistant → IllegalArgumentException")
        void userNotFound() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            StockMovementRequest request = StockMovementRequest.builder()
                    .type(StockMovement.MovementType.ENTRY)
                    .productId(1L)
                    .quantity(5)
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> stockMovementService.create(request, "unknown@test.com"));
        }
    }
}
