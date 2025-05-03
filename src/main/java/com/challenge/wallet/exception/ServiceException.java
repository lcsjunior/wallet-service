package com.challenge.wallet.exception;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class ServiceException extends RuntimeException {

    private final Response.Status status;

    public ServiceException(String message, Response.Status status) {
        super(message);
        this.status = status;
    }

    public ServiceException(String message) {
        super(message);
        this.status = BAD_REQUEST;
    }

    public Response.Status getStatus() {
        return status;
    }

    public record ErrorMessage(String message) {}

    public ErrorMessage toErrorMessage() {
        return new ErrorMessage(this.getMessage());
    }
}
