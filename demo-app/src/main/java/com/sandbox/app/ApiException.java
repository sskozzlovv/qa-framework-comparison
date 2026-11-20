package com.sandbox.app;

/** Turns into a JSON error response {"error": message} with the given HTTP status. */
final class ApiException extends RuntimeException {
    final int status;

    ApiException(int status, String message) {
        super(message);
        this.status = status;
    }
}
