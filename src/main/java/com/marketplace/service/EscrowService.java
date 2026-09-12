package com.marketplace.service;

import com.marketplace.enums.EscrowStatus;
import com.marketplace.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class EscrowService {

    private static final Logger log = LoggerFactory.getLogger(EscrowService.class);

    private final OrderItemRepository orderItemRepository;

    /**
     * Releases escrow-held funds whose hold period has expired.
     * Runs every hour. In production this could also trigger a vendor
     * payout-eligible event, but for now it just flips the status —
     * actual payout logic lives in the (future) Payout API.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void releaseExpiredEscrows() {
        int updatedCount = orderItemRepository.releaseEligibleEscrows(
                EscrowStatus.HOLDING,
                EscrowStatus.COMPLETED,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        if (updatedCount > 0) {
            log.info("Escrow released for {} order item(s)", updatedCount);
        }
    }
}