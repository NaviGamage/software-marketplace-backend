package com.marketplace.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ErrorResponse {

    private final boolean success = false;
    private final String message;
    private final Map<String, List<String>> fieldErrors;
    private final Instant timestamp;

    public ErrorResponse(String message, Map<String, List<String>> fieldErrors) {
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.timestamp = Instant.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}