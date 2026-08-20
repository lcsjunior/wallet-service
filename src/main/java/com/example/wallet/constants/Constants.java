package com.example.wallet.constants;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.UNNECESSARY;

import java.math.BigDecimal;

public final class Constants {

  private Constants() {}

  public static final String CORRELATION_ID_HEADER = "Correlation-Id";

  public static final String IDEMPOTENT_REPLAYED_HEADER = "Idempotent-Replayed";

  public static final BigDecimal ZERO_MONEY = ZERO.setScale(2, UNNECESSARY);
}
