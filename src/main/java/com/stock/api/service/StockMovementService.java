package com.stock.api.service;

import com.stock.api.dto.StockMovementRequest;
import com.stock.api.dto.StockMovementResponse;
import com.stock.api.entity.Product;
import com.stock.api.entity.StockMovement;
import com.stock.api.entity.StockMovement.MovementType;
import com.stock.api.entity.User;
import com.stock.api.repository.ProductRepository;
import com.stock.api.repository.StockMovementRepository;
import com.stock.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service de gestion des mouvements de stock (US-07, US-08).
 * RG-01 : quantité jamais négative.
 * RG-02 : rejet des sorties si quantité insuffisante.
 */
@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * US-08 : Historique paginé des mouvements d'un produit.
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> findByProductId(Long productId, Pageable pageable) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toResponse);
    }

    /**
     * US-08 : Historique filtrable (produit, type, dates).
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> findByFilters(Long productId, MovementType type,
                                                      LocalDateTime fromDate, LocalDateTime toDate,
                                                      Pageable pageable) {
        return stockMovementRepository.findByFilters(productId, type, fromDate, toDate, pageable)
                .map(this::toResponse);
    }

    /**
     * US-07 : Enregistrement d'un mouvement de stock.
     * RG-02 : rejet si sortie avec quantité insuffisante.
     * Met à jour automatiquement la quantité du produit.
     */
    @Transactional
    public StockMovementResponse create(StockMovementRequest request, String userEmail) {
        // Charger le produit
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Produit non trouvé avec l'id: " + request.getProductId()));

        if (product.isDeleted()) {
            throw new IllegalArgumentException("Produit supprimé");
        }

        // Charger l'utilisateur auteur par email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Utilisateur non trouvé avec l'email: " + userEmail));

        // RG-02 : vérifier la quantité disponible pour une sortie
        if (request.getType() == MovementType.EXIT) {
            if (!product.canRemoveQuantity(request.getQuantity())) {
                throw new IllegalStateException(
                        String.format("Quantité insuffisante pour le produit '%s'. " +
                                "Disponible: %d, Demandé: %d",
                                product.getName(), product.getQuantity(), request.getQuantity()));
            }
        }

        // Créer le mouvement
        StockMovement movement = StockMovement.builder()
                .type(request.getType())
                .product(product)
                .quantity(request.getQuantity())
                .reason(request.getReason())
                .performedBy(user)
                .build();

        movement = stockMovementRepository.save(movement);

        // Mettre à jour la quantité du produit (RG-01 : jamais négatif)
        if (request.getType() == MovementType.ENTRY) {
            product.addQuantity(request.getQuantity());
        } else {
            product.removeQuantity(request.getQuantity());
        }
        productRepository.save(product);

        return toResponse(movement);
    }

    private StockMovementResponse toResponse(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .type(movement.getType())
                .productId(movement.getProduct().getId())
                .productName(movement.getProduct().getName())
                .quantity(movement.getQuantity())
                .reason(movement.getReason())
                .performedById(movement.getPerformedBy().getId())
                .performedByEmail(movement.getPerformedBy().getEmail())
                .orderId(movement.getOrder() != null ? movement.getOrder().getId() : null)
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
