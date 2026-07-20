package com.example.wallet_service.service.exception;

public class SameWalletTransferException extends RuntimeException {

    public SameWalletTransferException(String userId) {
        super("Cannot transfer from wallet " + userId + " to itself");
    }
}
