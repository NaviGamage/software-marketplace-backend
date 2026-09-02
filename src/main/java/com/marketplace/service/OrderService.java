package com.marketplace.service;

import com.marketplace.dto.request.CreateOrderRequest;
import com.marketplace.dto.response.OrderItemResponse;
import com.marketplace.dto.response.OrderResponse;
import com.marketplace.entity.Order;
import com.marketplace.entity.OrderItem;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.EscrowStatus;
import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.ProductStatus;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.OrderItemRepository;
import com.marketplace.repository.OrderRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

import static com.marketplace.enums.LicenseType.REGULAR;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Value("${app.platform.commission-rate}")
    private BigDecimal commissionRate;

    @Value("${app.escrow.hold-days}")
    private int escrowHoldDays;

    /**
     * Checkout. NOTE: this is a mock checkout — it marks the order PAID
     * immediately with no real payment collected. This lets the rest of the
     * purchase → escrow → download flow be built and tested before Stripe
     * integration exists. Swapping in real payments later means: create the
     * order as PENDING here, and only flip it to PAID from a Stripe webhook
     * handler once payment is actually confirmed.
     */
    @Transactional
    public OrderResponse checkout(Long buyerId, CreateOrderRequest request) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", buyerId));

        Order order = Order.builder()
                .buyer(buyer)
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        OffsetDateTime releaseDate = OffsetDateTime.now().plusDays(escrowHoldDays);

        for (CreateOrderRequest.OrderLineItem line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", line.productId()));

            if (product.getStatus() != ProductStatus.APPROVED) {
                throw new BadRequestException(
                        "Product '" + product.getTitle() + "' is not available for purchase"
                );
            }

            BigDecimal price = switch (line.license()) {
                case REGULAR -> product.getPriceRegular();
                case EXTENDED -> product.getPriceExtended();
            };

            BigDecimal platformFee = price.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal vendorEarnings = price.subtract(platformFee);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .vendor(product.getVendor())
                    .license(line.license())
                    .price(price)
                    .platformFee(platformFee)
                    .vendorEarnings(vendorEarnings)
                    .escrowStatus(EscrowStatus.HOLDING)
                    .escrowReleaseDate(releaseDate)
                    .build();

            order.addOrderItem(item);
            total = total.add(price);
        }

        order.setTotalAmount(total);

        // Mock payment: mark as PAID immediately (see class-level note above).
        order.setStatus(OrderStatus.PAID);

        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Long buyerId, Pageable pageable) {
        return orderRepository.findByBuyerId(buyerId, pageable)
                .map(OrderResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<OrderItemResponse> getMySales(Long vendorId, Pageable pageable) {
        return orderItemRepository.findByVendorId(vendorId, pageable)
                .map(OrderItemResponse::fromEntity);
    }
}