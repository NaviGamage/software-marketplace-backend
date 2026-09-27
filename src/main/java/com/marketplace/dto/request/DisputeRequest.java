package com.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DisputeRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {
}
