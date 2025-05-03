package com.challenge.wallet.constants;

import java.math.BigDecimal;
import java.util.UUID;

public class Constants {

    public static final UUID NIL_UUID = new UUID(0L, 0L);

    public static final BigDecimal ONE_CENT = BigDecimal.valueOf(0.01);

    public static final BigDecimal ONE_TRILLION = BigDecimal.valueOf(1_000_000_000_000L);

    public static final int CURRENCY_DECIMAL_SCALE = 2;

    // NOSONAR
    private Constants() {
        throw new IllegalStateException();
    }
}