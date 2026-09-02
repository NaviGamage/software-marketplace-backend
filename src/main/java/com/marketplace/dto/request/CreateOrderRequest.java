package com.marketplace.dto.request;

import com.marketplace.enums.LicenseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<OrderLineItem> items
) {
    public record OrderLineItem(

            @NotNull(message = "Product ID is required")
            Long productId,

            @NotNull(message = "License type is required")
            LicenseType license
    ) {
    }
}