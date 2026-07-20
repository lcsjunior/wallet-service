package com.example.wallet.dto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(
    UUID walletId,
    @JsonFormat(shape = STRING) BigDecimal balance,
    @JsonFormat(shape = STRING) BigDecimal amount,
    String correlationId) {}
