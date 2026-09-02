package com.marketplace.repository;

import com.marketplace.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = {"product", "order", "order.buyer"})
    Page<OrderItem> findByVendorId(Long vendorId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "order"})
    Optional<OrderItem> findByIdAndOrderBuyerId(Long orderItemId, Long buyerId);
}