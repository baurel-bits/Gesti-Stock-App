package com.stock.api.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour l'entité Product.
 * RG-01 : quantité jamais négative.
 */
class ProductTest {

    private Product createProduct(int quantity) {
        return Product.builder()
                .name("Produit Test")
                .reference("REF-001")
                .quantity(quantity)
                .alertThreshold(10)
                .price(BigDecimal.valueOf(29.99))
                .category(Category.builder().name("Cat").build())
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // RG-01 : canRemoveQuantity
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("RG-01 — canRemoveQuantity()")
    class CanRemoveQuantityTests {

        @Test
        @DisplayName("Quantité suffisante → true")
        void sufficientQuantity() {
            Product product = createProduct(10);
            assertTrue(product.canRemoveQuantity(5));
        }

        @Test
        @DisplayName("Quantité exacte → true")
        void exactQuantity() {
            Product product = createProduct(10);
            assertTrue(product.canRemoveQuantity(10));
        }

        @Test
        @DisplayName("Quantité insuffisante → false")
        void insufficientQuantity() {
            Product product = createProduct(3);
            assertFalse(product.canRemoveQuantity(5));
        }

        @Test
        @DisplayName("Quantité zéro → false")
        void zeroQuantity() {
            Product product = createProduct(0);
            assertFalse(product.canRemoveQuantity(1));
        }
    }

    // ═══════════════════════════════════════════════════════
    // RG-01 : addQuantity
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("RG-01 — addQuantity()")
    class AddQuantityTests {

        @Test
        @DisplayName("Ajout positif → quantité augmentée")
        void addPositive() {
            Product product = createProduct(10);
            product.addQuantity(5);
            assertEquals(15, product.getQuantity());
        }

        @Test
        @DisplayName("Ajout à quantité zéro → quantité augmentée")
        void addFromZero() {
            Product product = createProduct(0);
            product.addQuantity(10);
            assertEquals(10, product.getQuantity());
        }

        @Test
        @DisplayName("Ajout de 0 → quantité inchangée")
        void addZero() {
            Product product = createProduct(10);
            product.addQuantity(0);
            assertEquals(10, product.getQuantity());
        }
    }

    // ═══════════════════════════════════════════════════════
    // RG-01 : removeQuantity — jamais négatif
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("RG-01 — removeQuantity() — jamais négatif")
    class RemoveQuantityTests {

        @Test
        @DisplayName("Retrait normal → quantité diminuée")
        void removeNormal() {
            Product product = createProduct(10);
            product.removeQuantity(3);
            assertEquals(7, product.getQuantity());
        }

        @Test
        @DisplayName("Retrait total → quantité à zéro")
        void removeAll() {
            Product product = createProduct(10);
            product.removeQuantity(10);
            assertEquals(0, product.getQuantity());
        }

        @Test
        @DisplayName("Retrait supérieur à la quantité → plafonné à 0 (jamais négatif)")
        void removeExceedsQuantity() {
            Product product = createProduct(3);
            product.removeQuantity(10);
            assertEquals(0, product.getQuantity(),
                    "RG-01 : la quantité ne doit jamais devenir négative");
        }

        @Test
        @DisplayName("Retrait depuis zéro → reste à 0 (jamais négatif)")
        void removeFromZero() {
            Product product = createProduct(0);
            product.removeQuantity(5);
            assertEquals(0, product.getQuantity(),
                    "RG-01 : la quantité ne doit jamais devenir négative");
        }

        @Test
        @DisplayName("Retrait négatif (-5) sur produit(2) → Math.max(0, 2-(-5)) = 7, jamais négatif")
        void removeNegativeNeverGoesBelowZero() {
            Product product = createProduct(2);
            product.removeQuantity(-5); // 2 - (-5) = 7, mais reste ≥ 0
            assertTrue(product.getQuantity() >= 0,
                    "RG-01 : la quantité reste toujours ≥ 0");
        }
    }
}
