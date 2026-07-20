package com.example.wallet.dto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(
    UUID fromWalletId,
    @JsonFormat(shape = STRING) BigDecimal fromBalance,
    UUID toWalletId,
    @JsonFormat(shape = STRING) BigDecimal toBalance,
    @JsonFormat(shape = STRING) BigDecimal amount,
    String correlationId) {}
