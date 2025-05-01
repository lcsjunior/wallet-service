package com.challenge.wallet.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricalBalanceBean(
        @JsonProperty("balance") BigDecimal balance,
        @JsonProperty("at") LocalDateTime at) {
}