package com.marketplace.dto.response;

import java.math.BigDecimal;

public record EarningsResponse(
        BigDecimal totalEarned,
        BigDecimal totalWithdrawn,
        BigDecimal pendingPayouts,
        BigDecimal availableBalance
) {
}