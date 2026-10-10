package com.marketplace.service;

import com.marketplace.dto.response.EarningsResponse;
import com.marketplace.dto.response.PayoutResponse;
import com.marketplace.entity.OrderItem;
import com.marketplace.entity.Payout;
import com.marketplace.entity.User;
import com.marketplace.enums.PayoutStatus;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.notification.NotificationService;
import com.marketplace.repository.OrderItemRepository;
import com.marketplace.repository.PayoutRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);

    private final PayoutRepository payoutRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.payout.minimum-amount}")
    private BigDecimal minimumPayoutAmount;

    @Transactional(readOnly = true)
    public EarningsResponse getEarnings(Long vendorId) {
        BigDecimal availableBalance = orderItemRepository.calculateAvailableBalance(vendorId);

        BigDecimal totalWithdrawn = payoutRepository.findByVendorId(vendorId, Pageable.unpaged())
                .stream()
                .filter(p -> p.getStatus() == PayoutStatus.PROCESSED)
                .map(Payout::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pending = payoutRepository.findByVendorId(vendorId, Pageable.unpaged())
                .stream()
                .filter(p -> p.getStatus() == PayoutStatus.PENDING)
                .map(Payout::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEarned = availableBalance.add(totalWithdrawn).add(pending);

        return new EarningsResponse(totalEarned, totalWithdrawn, pending, availableBalance);
    }

    /**
     * Requests a payout. Uses a pessimistic write lock on eligible order items
     * to prevent a race condition where two concurrent requests could both
     * read the same "available" balance and double-claim it.
     */
    @Transactional
    public PayoutResponse requestPayout(Long vendorId, BigDecimal requestedAmount) {
        if (requestedAmount.compareTo(minimumPayoutAmount) < 0) {
            throw new BadRequestException(
                    "Minimum payout amount is " + minimumPayoutAmount
            );
        }

        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", vendorId));

        // Locks eligible rows for the duration of this transaction.
        List<OrderItem> eligibleItems = orderItemRepository.findEligibleForPayoutWithLock(vendorId);

        BigDecimal availableBalance = eligibleItems.stream()
                .map(OrderItem::getVendorEarnings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (requestedAmount.compareTo(availableBalance) > 0) {
            throw new BadRequestException(
                    "Requested amount exceeds available balance of " + availableBalance
            );
        }

        // Create the payout record first so we have an id to link items to.
        Payout payout = Payout.builder()
                .vendor(vendor)
                .amount(requestedAmount)
                .status(PayoutStatus.PENDING)
                .build();
        payout = payoutRepository.save(payout);

        // Claim items (FIFO by escrow release date) until the requested amount is covered.
        BigDecimal running = BigDecimal.ZERO;
        List<OrderItem> claimed = new ArrayList<>();
        for (OrderItem item : eligibleItems) {
            if (running.compareTo(requestedAmount) >= 0) {
                break;
            }
            item.setPayout(payout);
            claimed.add(item);
            running = running.add(item.getVendorEarnings());
        }
        orderItemRepository.saveAll(claimed);

        log.info("Vendor {} requested payout of {} ({} order items claimed)",
                vendorId, requestedAmount, claimed.size());

        return PayoutResponse.fromEntity(payout);
    }

    @Transactional(readOnly = true)
    public Page<PayoutResponse> getMyPayouts(Long vendorId, Pageable pageable) {
        return payoutRepository.findByVendorId(vendorId, pageable)
                .map(PayoutResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PayoutResponse> getAllPayouts(Pageable pageable) {
        return payoutRepository.findAll(pageable)
                .map(PayoutResponse::fromEntity);
    }

    @Transactional
    public PayoutResponse approvePayout(Long payoutId) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout", payoutId));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new BadRequestException("Only PENDING payouts can be approved");
        }

        payout.setStatus(PayoutStatus.PROCESSED);
        payout.setProcessedAt(OffsetDateTime.now());
        payoutRepository.save(payout);
        notificationService.payoutApproved(payout.getVendor(), payout.getAmount());

        log.info("Payout {} approved for vendor {}", payoutId, payout.getVendor().getId());
        return PayoutResponse.fromEntity(payout);
    }

    /**
     * Rejecting a payout releases the claimed order items back to the pool
     * (payout = null) so the vendor's balance becomes available again.
     */
    @Transactional
    public PayoutResponse rejectPayout(Long payoutId, String reason) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout", payoutId));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new BadRequestException("Only PENDING payouts can be rejected");
        }

        payout.setStatus(PayoutStatus.REJECTED);
        payout.setRejectionReason(reason);
        payout.setProcessedAt(OffsetDateTime.now());
        payoutRepository.save(payout);

        // Release claimed items back into the available pool.
        List<OrderItem> claimedItems = orderItemRepository.findByPayoutId(payoutId);
        claimedItems.forEach(item -> item.setPayout(null));
        orderItemRepository.saveAll(claimedItems);
        notificationService.payoutRejected(payout.getVendor(), payout.getAmount(), reason);
        log.info("Payout {} rejected for vendor {}: {}", payoutId, payout.getVendor().getId(), reason);
        return PayoutResponse.fromEntity(payout);
    }
}