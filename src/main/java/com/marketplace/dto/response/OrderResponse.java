package com.marketplace.dto.response;

import com.marketplace.entity.Order;
import com.marketplace.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        BigDecimal totalAmount,
        OrderStatus status,
        List<OrderItemResponse> items,
        OffsetDateTime createdAt
) {
    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getItems().stream().map(OrderItemResponse::fromEntity).toList(),
                order.getCreatedAt()
        );
    }
}