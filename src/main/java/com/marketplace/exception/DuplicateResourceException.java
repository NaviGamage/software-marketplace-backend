package com.marketplace.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApplicationException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}