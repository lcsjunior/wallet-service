package com.challenge.wallet.constants;

import java.util.UUID;

public class TestConstants {

    public static UUID NIL_UUID = new UUID(0L, 0L);

    public static final UUID TEST_UUID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public static final UUID TEST_UUID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private TestConstants() {
        throw new IllegalStateException();
    }
}
