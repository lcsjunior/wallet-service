package com.example.wallet.dto;

public enum TransactionOutcome {
  APPLIED,
  REPLAYED;

  public boolean isReplayed() {
    return this == REPLAYED;
  }
}
