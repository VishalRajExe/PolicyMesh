package com.policymesh.ci.backend;

/**
 * Exception thrown when backend access fails.
 * Results in exit code 3.
 */
public class BackendAccessException extends Exception {

    public BackendAccessException(String message) {
        super(message);
    }

    public BackendAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
