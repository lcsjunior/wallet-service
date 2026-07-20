package com.example.wallet_service.service.exception;

public class WalletAlreadyExistsException extends RuntimeException {

    public WalletAlreadyExistsException(String userId) {
        super("Wallet already exists for user " + userId);
    }
}
