package com.marketplace.controller;

import com.marketplace.dto.request.CreatePayoutRequest;
import com.marketplace.dto.response.ApiResponse;
import com.marketplace.dto.response.EarningsResponse;
import com.marketplace.dto.response.PayoutResponse;
import com.marketplace.security.UserPrincipal;
import com.marketplace.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
public class VendorController {

    private final PayoutService payoutService;

    @GetMapping("/earnings")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<EarningsResponse>> getEarnings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        EarningsResponse response = payoutService.getEarnings(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/payouts")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<PayoutResponse>> requestPayout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePayoutRequest request
    ) {
        PayoutResponse response = payoutService.requestPayout(principal.getId(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payout requested successfully"));
    }

    @GetMapping("/payouts")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Page<PayoutResponse>>> getMyPayouts(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable
    ) {
        Page<PayoutResponse> response = payoutService.getMyPayouts(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}