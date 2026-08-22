package com.policymesh.ci.parser;

/**
 * Exception thrown when policy parsing fails.
 * This results in exit code 2 (configuration error).
 */
public class PolicyParseException extends Exception {

    public PolicyParseException(String message) {
        super(message);
    }

    public PolicyParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
