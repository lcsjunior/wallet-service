package com.example.wallet_service.service.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String userId) {
        super("Wallet " + userId + " has insufficient balance for this operation");
    }
}
