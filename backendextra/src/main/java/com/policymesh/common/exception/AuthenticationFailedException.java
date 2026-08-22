package com.policymesh.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown on bad credentials / invalid tokens. */
public class AuthenticationFailedException extends PolicyMeshException {
    public AuthenticationFailedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "https://policymesh/errors/authentication-failed");
    }
}
