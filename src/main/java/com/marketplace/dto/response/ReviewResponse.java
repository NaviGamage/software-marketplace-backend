package com.marketplace.dto.response;

import com.marketplace.entity.Review;

import java.time.OffsetDateTime;

public record ReviewResponse(
        Long id,
        String buyerName,
        int rating,
        String comment,
        OffsetDateTime createdAt
) {
    public static ReviewResponse fromEntity(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBuyer().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}