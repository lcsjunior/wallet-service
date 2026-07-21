package com.example.wallet.dto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(UUID walletId, @JsonFormat(shape = STRING) BigDecimal balance) {

  public static BalanceResponse of(UUID walletId, BigDecimal balance) {
    return new BalanceResponse(walletId, balance);
  }
}
