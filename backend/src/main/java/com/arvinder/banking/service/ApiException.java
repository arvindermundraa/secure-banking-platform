package com.arvinder.banking.service;

import org.springframework.http.HttpStatus;

/** Business error with the HTTP status the API should return. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
