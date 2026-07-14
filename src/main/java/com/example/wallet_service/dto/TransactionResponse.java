package com.example.wallet_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record TransactionResponse(
        String userId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        String correlationId) {
}
