package com.challenge.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawRequest(
        @NotNull UUID walletId,
        @Schema(defaultValue = "0.01")
        @NotNull @Positive BigDecimal amount) {
}