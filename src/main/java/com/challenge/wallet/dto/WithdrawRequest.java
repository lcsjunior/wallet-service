package com.challenge.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawRequest(
        @NotNull UUID walletId,
        @NotNull @Positive BigDecimal amount) {
}