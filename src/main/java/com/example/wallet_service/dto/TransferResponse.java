package com.example.wallet_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record TransferResponse(
        String fromUserId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal fromBalance,
        String toUserId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal toBalance,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        String correlationId) {
}
