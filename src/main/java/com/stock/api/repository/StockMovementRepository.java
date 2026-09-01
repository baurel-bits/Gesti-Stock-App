package com.stock.api.repository;

import com.stock.api.entity.StockMovement;
import com.stock.api.entity.StockMovement.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    List<StockMovement> findByProductIdAndTypeOrderByCreatedAtDesc(Long productId, MovementType type);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.product.id = :productId " +
            "AND (:type IS NULL OR sm.type = :type) " +
            "AND (:fromDate IS NULL OR sm.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR sm.createdAt <= :toDate) " +
            "ORDER BY sm.createdAt DESC")
    Page<StockMovement> findByFilters(@Param("productId") Long productId,
                                       @Param("type") MovementType type,
                                       @Param("fromDate") LocalDateTime fromDate,
                                       @Param("toDate") LocalDateTime toDate,
                                       Pageable pageable);
}
