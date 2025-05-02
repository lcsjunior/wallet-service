package com.challenge.wallet.dto;

import com.challenge.wallet.bean.HistoricalBalanceBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricalBalanceResponse(
        BigDecimal balance,
        LocalDateTime at) {

    public static HistoricalBalanceResponse from(HistoricalBalanceBean historicalBalanceBean) {
        return new HistoricalBalanceResponse(historicalBalanceBean.balance(), historicalBalanceBean.at());
    }
}
