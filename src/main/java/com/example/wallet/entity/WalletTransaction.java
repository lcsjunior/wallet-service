package com.example.wallet.entity;

import static jakarta.persistence.EnumType.STRING;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "wallet_transaction",
    indexes =
        @Index(
            name = "idx_wallet_transaction_wallet_created",
            columnList = "wallet_id, created_at"))
public class WalletTransaction {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "wallet_id", nullable = false, updatable = false)
  private UUID walletId;

  @Enumerated(STRING)
  @Column(name = "type", nullable = false, updatable = false, length = 20)
  private TransactionType type;

  @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "balance_after", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal balanceAfter;

  @Column(name = "correlation_id", nullable = false, updatable = false)
  private String correlationId;

  @Column(name = "counterparty_wallet_id", updatable = false)
  private UUID counterpartyWalletId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected WalletTransaction() {}

  private WalletTransaction(
      UUID id,
      UUID walletId,
      TransactionType type,
      BigDecimal amount,
      BigDecimal balanceAfter,
      String correlationId,
      UUID counterpartyWalletId,
      Instant createdAt) {
    this.id = id;
    this.walletId = walletId;
    this.type = type;
    this.amount = amount;
    this.balanceAfter = balanceAfter;
    this.correlationId = correlationId;
    this.counterpartyWalletId = counterpartyWalletId;
    this.createdAt = createdAt;
  }

  public static WalletTransaction of(
      UUID walletId,
      TransactionType type,
      BigDecimal amount,
      BigDecimal balanceAfter,
      String correlationId,
      UUID counterpartyWalletId) {
    return new WalletTransaction(
        UUID.randomUUID(),
        walletId,
        type,
        amount,
        balanceAfter,
        correlationId,
        counterpartyWalletId,
        Instant.now());
  }

  public UUID getId() {
    return id;
  }

  public UUID getWalletId() {
    return walletId;
  }

  public TransactionType getType() {
    return type;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getBalanceAfter() {
    return balanceAfter;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public UUID getCounterpartyWalletId() {
    return counterpartyWalletId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
