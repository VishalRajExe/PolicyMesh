package com.policymesh.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested resource (policy, service, decision, etc.) does not exist. */
public class ResourceNotFoundException extends PolicyMeshException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "https://policymesh/errors/not-found");
    }
}
