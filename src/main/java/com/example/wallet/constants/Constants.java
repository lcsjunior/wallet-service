package com.example.wallet.constants;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.UNNECESSARY;

import java.math.BigDecimal;

public final class Constants {

  public static final BigDecimal ZERO_AMOUNT = ZERO.setScale(2, UNNECESSARY);

  private Constants() {}
}
