package com.marketplace.service;

import com.marketplace.dto.response.DisputeResponse;
import com.marketplace.entity.OrderItem;
import com.marketplace.enums.EscrowStatus;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.ForbiddenException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);

    private final OrderItemRepository orderItemRepository;

    /**
     * Buyer opens a dispute on a purchased item. Only allowed while the item
     * is still HOLDING or COMPLETED escrow AND has not yet been claimed by a
     * vendor payout (payout == null). Once a payout has claimed the item,
     * disputing it would require a separate clawback process (out of scope).
     */
    @Transactional
    public DisputeResponse openDispute(Long orderItemId, Long buyerId, String reason) {
        OrderItem item = orderItemRepository.findWithOrderAndProductById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        if (!item.getOrder().getBuyer().getId().equals(buyerId)) {
            throw new ForbiddenException("You do not have access to this order item");
        }

        if (item.getEscrowStatus() != EscrowStatus.HOLDING && item.getEscrowStatus() != EscrowStatus.COMPLETED) {
            throw new BadRequestException("This item cannot be disputed in its current state");
        }

        if (item.getPayout() != null) {
            throw new BadRequestException(
                    "This item has already been included in a vendor payout and cannot be disputed automatically. Please contact support."
            );
        }

        item.setEscrowStatus(EscrowStatus.DISPUTED);
        item.setDisputeReason(reason);
        item.setDisputedAt(OffsetDateTime.now());
        orderItemRepository.save(item);

        log.info("Dispute opened for order item {} by buyer {}", orderItemId, buyerId);
        return DisputeResponse.fromEntity(item);
    }

    @Transactional(readOnly = true)
    public Page<DisputeResponse> getOpenDisputes(Pageable pageable) {
        return orderItemRepository.findByEscrowStatus(EscrowStatus.DISPUTED, pageable)
                .map(DisputeResponse::fromEntity);
    }

    /**
     * Admin resolves a dispute:
     * - approve = true  -> refund: escrow marked REFUNDED, buyer loses download access
     * - approve = false -> dismiss: escrow reverts to HOLDING or COMPLETED
     *   depending on whether the original hold period has already elapsed
     */
    @Transactional
    public DisputeResponse resolveDispute(Long orderItemId, boolean approve, String note) {
        OrderItem item = orderItemRepository.findWithOrderAndProductById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        if (item.getEscrowStatus() != EscrowStatus.DISPUTED) {
            throw new BadRequestException("This item does not have an open dispute");
        }

        if (approve) {
            item.setEscrowStatus(EscrowStatus.REFUNDED);
        } else {
            boolean holdExpired = item.getEscrowReleaseDate() != null
                    && !item.getEscrowReleaseDate().isAfter(OffsetDateTime.now());
            item.setEscrowStatus(holdExpired ? EscrowStatus.COMPLETED : EscrowStatus.HOLDING);
        }

        item.setResolutionNote(note);
        item.setResolvedAt(OffsetDateTime.now());
        orderItemRepository.save(item);

        log.info("Dispute {} for order item {}: {}",
                approve ? "APPROVED (refunded)" : "DISMISSED", orderItemId, note);
        return DisputeResponse.fromEntity(item);
    }
}