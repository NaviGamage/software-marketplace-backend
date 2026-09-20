package com.marketplace.controller;

import com.marketplace.dto.response.ApiResponse;
import com.marketplace.dto.response.PayoutResponse;
import com.marketplace.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPayoutController {

    private final PayoutService payoutService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PayoutResponse>>> getAllPayouts(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(payoutService.getAllPayouts(pageable)));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PayoutResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(payoutService.approvePayout(id)));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<PayoutResponse>> reject(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body
    ) {
        String reason = body != null ? body.getOrDefault("reason", "Not specified") : "Not specified";
        return ResponseEntity.ok(ApiResponse.success(payoutService.rejectPayout(id, reason)));
    }
}