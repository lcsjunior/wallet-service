package com.challenge.wallet.dto;

import com.challenge.wallet.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceResponse(
        BigDecimal balance,
        LocalDateTime updatedAt) {

    public static BalanceResponse from(Wallet wallet) {
        return new BalanceResponse(wallet.getBalance(), wallet.getUpdatedAt());
    }
}
