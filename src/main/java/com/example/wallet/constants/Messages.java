package com.example.wallet.constants;

public final class Messages {

  private Messages() {}

  public static final String WALLET_NOT_FOUND = "Wallet not found";

  public static final String INSUFFICIENT_BALANCE = "Insufficient balance";

  public static final String CORRELATION_ID_CONFLICT =
      "Correlation-Id already used with different parameters";

  public static final String SAME_WALLET_TRANSFER = "Cannot transfer to the same wallet";

  public static final String MISSING_REQUIRED_HEADER = "Missing %s header";

  public static final String VALIDATION_ERROR = "One or more fields are invalid";
}
