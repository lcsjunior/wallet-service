package com.challenge.wallet.constants;

public enum ValidationMessages {
    MSG_ENTITY_NOT_FOUND(" not found"),
    MSG_INVALID_DATETIME_FORMAT("Invalid datetime format. Use ISO format like '2025-05-01T15:30:00'"),
    MSG_AMOUNT_MINIMUM_REQUIRED("Amount must be greater than or equal to 0.01"),
    MSG_AMOUNT_INVALID_DECIMAL_SCALE("Amount must have at most two decimal places"),
    MSG_INSUFFICIENT_BALANCE("Insufficient balance"),
    MSG_CANNOT_TRANSFER_TO_SAME_WALLET("Cannot transfer to the same wallet");

    private final String message;

    ValidationMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}