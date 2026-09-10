package com.marketplace.dto.response;

import java.time.Instant;

public record DownloadResponse(
        String productTitle,
        String version,
        String downloadUrl,
        Instant expiresAt
) {
}