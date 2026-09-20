package com.marketplace.dto.response;

import com.marketplace.entity.Payout;
import com.marketplace.enums.PayoutStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PayoutResponse(
        Long id,
        BigDecimal amount,
        PayoutStatus status,
        String rejectionReason,
        OffsetDateTime requestedAt,
        OffsetDateTime processedAt
) {
    public static PayoutResponse fromEntity(Payout payout) {
        return new PayoutResponse(
                payout.getId(),
                payout.getAmount(),
                payout.getStatus(),
                payout.getRejectionReason(),
                payout.getCreatedAt(),
                payout.getProcessedAt()
        );
    }
}