package com.marketplace.service;

import com.marketplace.dto.response.ReviewResponse;
import com.marketplace.entity.Product;
import com.marketplace.entity.Review;
import com.marketplace.entity.User;
import com.marketplace.enums.EscrowStatus;
import com.marketplace.enums.OrderStatus;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.DuplicateResourceException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.OrderItemRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.ReviewRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);


    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse createReview(Long productId, Long buyerId, int rating, String comment) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));

        if (reviewRepository.existsByProductIdAndBuyerId(productId, buyerId)) {
            throw new DuplicateResourceException("You have already reviewed this product");
        }

        boolean hasVerifiedPurchase = orderItemRepository
                .existsVerifiedPurchase(productId, buyerId, OrderStatus.PAID, EscrowStatus.REFUNDED);

        if (!hasVerifiedPurchase) {
            throw new BadRequestException("You can only review products you have purchased");
        }

        Review review = Review.builder()
                .product(product)
                .buyer(buyer)
                .rating(rating)
                .comment(comment)
                .build();
        review = reviewRepository.save(review);

        productRepository.recalculateRating(productId);

        log.info("Review created for product {} by buyer {} (rating: {})", productId, buyerId, rating);
        return ReviewResponse.fromEntity(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(ReviewResponse::fromEntity);
    }
}