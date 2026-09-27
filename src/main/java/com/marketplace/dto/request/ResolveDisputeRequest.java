package com.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;

public record ResolveDisputeRequest(
        @NotNull(message = "approve is required")
        Boolean approve,   // true = refund buyer, false = dismiss dispute
        String note
) {
}