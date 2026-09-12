package com.marketplace.repository;

import com.marketplace.entity.OrderItem;
import com.marketplace.enums.EscrowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = {"product", "order", "order.buyer"})
    Page<OrderItem> findByVendorId(Long vendorId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "order"})
    Optional<OrderItem> findByIdAndOrderBuyerId(Long orderItemId, Long buyerId);

    @EntityGraph(attributePaths = {"order", "order.buyer", "product"})
    Optional<OrderItem> findWithOrderAndProductById(Long id);

    // existing methods walata add karanna:
    List<OrderItem> findByEscrowStatusAndEscrowReleaseDateLessThanEqual(
            EscrowStatus escrowStatus, OffsetDateTime now);

    @Modifying
    @Query("UPDATE OrderItem o SET o.escrowStatus = :newStatus WHERE o.escrowStatus = :currentStatus AND o.escrowReleaseDate <= :now")
    int releaseEligibleEscrows(
            @Param("currentStatus") EscrowStatus currentStatus,
            @Param("newStatus") EscrowStatus newStatus,
            @Param("now") OffsetDateTime now
    );
}