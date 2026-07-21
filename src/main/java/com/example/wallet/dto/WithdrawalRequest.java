package com.example.wallet.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record WithdrawalRequest(
    @NotNull
        @Positive(message = "{amount.positive}")
        @Digits(integer = 17, fraction = 2, message = "{amount.scale}")
        BigDecimal amount) {}
