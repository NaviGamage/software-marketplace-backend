package com.marketplace.dto.response;

import com.marketplace.entity.OrderItem;
import com.marketplace.enums.EscrowStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DisputeResponse(
        Long orderItemId,
        String productTitle,
        BigDecimal price,
        EscrowStatus escrowStatus,
        String disputeReason,
        OffsetDateTime disputedAt,
        String resolutionNote,
        OffsetDateTime resolvedAt
) {
    public static DisputeResponse fromEntity(OrderItem item) {
        return new DisputeResponse(
                item.getId(),
                item.getProduct().getTitle(),
                item.getPrice(),
                item.getEscrowStatus(),
                item.getDisputeReason(),
                item.getDisputedAt(),
                item.getResolutionNote(),
                item.getResolvedAt()
        );
    }
}