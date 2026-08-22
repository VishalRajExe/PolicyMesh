package com.policymesh.ci.engine;

/**
 * Exception thrown when the service graph is structurally invalid.
 */
public class GraphValidationException extends Exception {

    public GraphValidationException(String message) {
        super(message);
    }

    public GraphValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
