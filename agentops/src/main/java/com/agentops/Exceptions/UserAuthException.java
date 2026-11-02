package com.agentops.Exceptions;

/**
 * Thrown for authentication and authorization failures (duplicate email, bad credentials, etc.)
 * Extends RuntimeException so it propagates unchecked and is caught by the global handler.
 */
public class UserAuthException extends RuntimeException {

    public UserAuthException(String message) {
        super(message);
    }
}
