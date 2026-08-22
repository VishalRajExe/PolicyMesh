package com.policymesh.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a policy DSL document is malformed or fails compilation. */
public class InvalidPolicyException extends PolicyMeshException {
    public InvalidPolicyException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "https://policymesh/errors/invalid-policy");
    }
}
