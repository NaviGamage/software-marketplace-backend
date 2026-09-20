package com.marketplace.repository;

import com.marketplace.entity.Payout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRepository extends JpaRepository<Payout, Long> {
    Page<Payout> findByVendorId(Long vendorId, Pageable pageable);
    Page<Payout> findByStatus(com.marketplace.enums.PayoutStatus status, Pageable pageable);
}