package com.challenge.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public record HistoricalBalanceQuery(
        @QueryParam("at") @DefaultValue("2025-05-01T12:00:00") @NotNull String at) {
}