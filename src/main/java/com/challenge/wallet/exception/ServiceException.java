package com.challenge.wallet.exception;

import jakarta.ws.rs.core.Response;

public class ServiceException extends RuntimeException {

    private final Response.Status status;

    public ServiceException(String message, Response.Status status) {
        super(message);
        this.status = status;
    }

    public Response.Status getStatus() {
        return status;
    }

    public static class ErrorMessage {
        private String message;
        public String getMessage() {
            return message;
        }
    }

    public ErrorMessage toErrorMessage() {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.message = this.getMessage();
        return errorMessage;
    }
}
