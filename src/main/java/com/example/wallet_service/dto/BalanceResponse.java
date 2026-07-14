package com.example.wallet_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(
        String userId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance,
        Instant asOf) {
}
