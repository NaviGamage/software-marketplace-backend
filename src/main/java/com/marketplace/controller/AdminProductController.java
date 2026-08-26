package com.marketplace.controller;

import com.marketplace.dto.request.RejectProductRequest;
import com.marketplace.dto.response.ApiResponse;
import com.marketplace.dto.response.ProductResponse;
import com.marketplace.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getPendingProducts(Pageable pageable) {
        Page<ProductResponse> products = productService.getPendingProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ProductResponse>> approveProduct(@PathVariable Long id) {
        ProductResponse approved = productService.approveProduct(id);
        return ResponseEntity.ok(ApiResponse.success(approved, "Product approved successfully"));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ProductResponse>> rejectProduct(
            @PathVariable Long id,
            @Valid @RequestBody RejectProductRequest request
    ) {
        ProductResponse rejected = productService.rejectProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(rejected, "Product rejected"));
    }
}