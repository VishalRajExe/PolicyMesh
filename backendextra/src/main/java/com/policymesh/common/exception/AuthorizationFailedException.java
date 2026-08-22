package com.policymesh.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an authenticated user lacks permission for an operation. */
public class AuthorizationFailedException extends PolicyMeshException {
    public AuthorizationFailedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "https://policymesh/errors/authorization-failed");
    }
}
