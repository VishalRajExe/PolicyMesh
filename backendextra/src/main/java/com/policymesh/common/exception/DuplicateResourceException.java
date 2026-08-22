package com.policymesh.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when attempting to create a resource that already exists (e.g. duplicate policy code). */
public class DuplicateResourceException extends PolicyMeshException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "https://policymesh/errors/duplicate");
    }
}
