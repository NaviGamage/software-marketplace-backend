package com.marketplace.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException {

  public ResourceNotFoundException(String resourceName, Object identifier) {
    super(resourceName + " not found with identifier: " + identifier, HttpStatus.NOT_FOUND);
  }

  public ResourceNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND);
  }
}