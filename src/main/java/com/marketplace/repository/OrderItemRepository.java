package com.marketplace.repository;

import com.marketplace.entity.OrderItem;
import com.marketplace.enums.EscrowStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;


import java.math.BigDecimal;
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


    List<OrderItem> findByEscrowStatusAndEscrowReleaseDateLessThanEqual(
            EscrowStatus escrowStatus, OffsetDateTime now);



    List<OrderItem> findByPayoutId(Long payoutId);

    Page<OrderItem> findByEscrowStatus(EscrowStatus escrowStatus, Pageable pageable);

    @Modifying
    @Query("UPDATE OrderItem o SET o.escrowStatus = :newStatus WHERE o.escrowStatus = :currentStatus AND o.escrowReleaseDate <= :now")
    int releaseEligibleEscrows(
            @Param("currentStatus") EscrowStatus currentStatus,
            @Param("newStatus") EscrowStatus newStatus,
            @Param("now") OffsetDateTime now
    );

    @Query("SELECT COALESCE(SUM(o.vendorEarnings), 0) FROM OrderItem o " +
            "WHERE o.vendor.id = :vendorId AND o.escrowStatus = com.marketplace.enums.EscrowStatus.COMPLETED " +
            "AND o.payout IS NULL")
    BigDecimal calculateAvailableBalance(@Param("vendorId") Long vendorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderItem o " +
            "WHERE o.vendor.id = :vendorId AND o.escrowStatus = com.marketplace.enums.EscrowStatus.COMPLETED " +
            "AND o.payout IS NULL " +
            "ORDER BY o.escrowReleaseDate ASC")
    List<OrderItem> findEligibleForPayoutWithLock(@Param("vendorId") Long vendorId);
}