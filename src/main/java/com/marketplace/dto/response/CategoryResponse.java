package com.marketplace.dto.response;

import com.marketplace.entity.Category;

import java.time.OffsetDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        OffsetDateTime createdAt
) {
    public static CategoryResponse fromEntity(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getCreatedAt()
        );
    }
}