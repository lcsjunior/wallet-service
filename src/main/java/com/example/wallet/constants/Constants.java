package com.example.wallet.constants;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.UNNECESSARY;

import java.math.BigDecimal;

public final class Constants {

  private Constants() {}

  public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

  public static final String CORRELATION_ID_HEADER = "Correlation-ID";

  public static final String CORRELATION_ID_MDC_KEY = "correlationId";

  public static final String IDEMPOTENCY_KEY_MDC_KEY = "idempotencyKey";

  public static final BigDecimal ZERO_MONEY = ZERO.setScale(2, UNNECESSARY);
}
