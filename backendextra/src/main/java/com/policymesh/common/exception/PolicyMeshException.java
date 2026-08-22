package com.policymesh.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base unchecked exception for all PolicyMesh domain errors.
 * Carries an HTTP status and a machine-readable error "type" slug
 * so the global exception handler can build RFC 7807 responses.
 */
public class PolicyMeshException extends RuntimeException {

    private final HttpStatus status;
    private final String type;

    public PolicyMeshException(String message, HttpStatus status, String type) {
        super(message);
        this.status = status;
        this.type = type;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }
}
