package com.challenge.wallet.constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TestConstants {

    public static final BigDecimal INVALID_DECIMAL_AMOUNT = BigDecimal.valueOf(0.999);

    public static final BigDecimal ONE_TRILLION = BigDecimal.valueOf(1_000_000_000_000L);

    public static final LocalDateTime TEST_MIN_DATE = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
    public static final LocalDateTime TEST_MAX_DATE = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    // NOSONAR
    private TestConstants() {
        throw new IllegalStateException();
    }
}
