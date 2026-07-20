package com.example.wallet_service.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String fromUserId,
        @NotBlank String toUserId,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
