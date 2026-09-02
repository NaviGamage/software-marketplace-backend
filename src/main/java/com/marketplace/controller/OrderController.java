package com.marketplace.controller;

import com.marketplace.dto.request.CreateOrderRequest;
import com.marketplace.dto.response.ApiResponse;
import com.marketplace.dto.response.OrderItemResponse;
import com.marketplace.dto.response.OrderResponse;
import com.marketplace.security.UserPrincipal;
import com.marketplace.service.OrderService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse order = orderService.checkout(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order placed successfully"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable
    ) {
        Page<OrderResponse> orders = orderService.getMyOrders(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/my-sales")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Page<OrderItemResponse>>> getMySales(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable
    ) {
        Page<OrderItemResponse> sales = orderService.getMySales(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(sales));
    }
}