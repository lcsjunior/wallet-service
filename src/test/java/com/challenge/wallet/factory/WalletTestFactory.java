package com.challenge.wallet.factory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WalletTestFactory {

    public static String toIsoString(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    private WalletTestFactory() {
        throw new IllegalStateException();
    }
}
