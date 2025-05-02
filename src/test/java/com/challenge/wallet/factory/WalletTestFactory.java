package com.challenge.wallet.factory;

import com.challenge.wallet.dto.HistoricalBalanceQuery;
import com.challenge.wallet.model.Wallet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.challenge.wallet.constants.TestConstants.TEST_MAX_DATE;

public class WalletTestFactory {

    public static String toIsoString(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public static Wallet createWallet() {
        return new Wallet();
    }

    public static HistoricalBalanceQuery createHistoricalBalanceQuery() {
        return new HistoricalBalanceQuery(toIsoString(TEST_MAX_DATE));
    }

    // NOSONAR
    private WalletTestFactory() {
        throw new IllegalStateException();
    }
}
