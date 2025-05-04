package com.challenge.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID fromWalletId,
        @NotNull UUID toWalletId,
        @Schema(defaultValue = "0.01")
        @NotNull @PositiveOrZero BigDecimal amount) {
}
