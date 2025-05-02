package com.challenge.wallet.constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TestConstants {

    public static final UUID TEST_UUID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID TEST_UUID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID TEST_UUID_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    public static final LocalDateTime TEST_MAX_DATE = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    public static final UUID NIL_UUID = new UUID(0L, 0L);

    public static final BigDecimal INVALID_CURRENCY_SCALE = BigDecimal.valueOf(0.999);

    // NOSONAR
    private TestConstants() {
        throw new IllegalStateException();
    }
}
