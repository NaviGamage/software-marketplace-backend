package com.marketplace.dto.response;

import com.marketplace.entity.OrderItem;
import com.marketplace.enums.EscrowStatus;
import com.marketplace.enums.LicenseType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderItemResponse(
        Long id,
        ProductSummary product,
        LicenseType license,
        BigDecimal price,
        EscrowStatus escrowStatus,
        OffsetDateTime escrowReleaseDate,
        boolean downloaded
) {
    public static OrderItemResponse fromEntity(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                new ProductSummary(item.getProduct().getId(), item.getProduct().getTitle(), item.getProduct().getSlug()),
                item.getLicense(),
                item.getPrice(),
                item.getEscrowStatus(),
                item.getEscrowReleaseDate(),
                item.isDownloaded()
        );
    }

    public record ProductSummary(Long id, String title, String slug) {
    }
}