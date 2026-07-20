package com.example.wallet.dto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
    UUID walletId,
    UUID userId,
    @JsonFormat(shape = STRING) BigDecimal balance,
    Instant createdAt) {}
