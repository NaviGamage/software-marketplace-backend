package com.marketplace.controller;

import com.marketplace.dto.request.ResolveDisputeRequest;
import com.marketplace.dto.response.ApiResponse;
import com.marketplace.dto.response.DisputeResponse;
import com.marketplace.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/disputes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDisputeController {

    private final DisputeService disputeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DisputeResponse>>> getOpenDisputes(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(disputeService.getOpenDisputes(pageable)));
    }

    @PatchMapping("/{orderItemId}/resolve")
    public ResponseEntity<ApiResponse<DisputeResponse>> resolve(
            @PathVariable Long orderItemId,
            @Valid @RequestBody ResolveDisputeRequest request
    ) {
        DisputeResponse response = disputeService.resolveDispute(
                orderItemId, request.approve(), request.note()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
