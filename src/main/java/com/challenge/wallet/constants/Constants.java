package com.challenge.wallet.constants;

import java.math.BigDecimal;

public class Constants {

    public static final BigDecimal ONE_CENT = BigDecimal.valueOf(0.01);

    public static final BigDecimal ONE_TRILLION = BigDecimal.valueOf(1_000_000_000_000L);

    private Constants() {
        throw new IllegalStateException();
    }
}