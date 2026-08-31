package com.marketplace.dto.response;

import com.marketplace.entity.Product;
import com.marketplace.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductResponse(
        Long id,
        VendorSummary vendor,
        CategorySummary category,
        String title,
        String slug,
        String description,
        String demoUrl,
        BigDecimal priceRegular,
        BigDecimal priceExtended,
        String currentVersion,
        boolean hasFile,
        ProductStatus status,
        String rejectionReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                new VendorSummary(product.getVendor().getId(), product.getVendor().getFullName()),
                new CategorySummary(product.getCategory().getId(), product.getCategory().getName()),
                product.getTitle(),
                product.getSlug(),
                product.getDescription(),
                product.getDemoUrl(),
                product.getPriceRegular(),
                product.getPriceExtended(),
                product.getCurrentVersion(),
                product.getFileKey() != null,
                product.getStatus(),
                product.getRejectionReason(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    // Nested summaries instead of embedding full UserResponse/CategoryResponse —
    // a product listing needs the vendor's name, not their email or account status.
    public record VendorSummary(Long id, String fullName) {
    }

    public record CategorySummary(Long id, String name) {
    }
}