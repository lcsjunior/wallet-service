package com.example.wallet_service.service.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String correlationId) {
        super("Correlation-Id " + correlationId + " was already used with different request parameters");
    }
}
