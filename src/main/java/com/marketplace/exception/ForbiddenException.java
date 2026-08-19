package com.marketplace.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApplicationException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}