package com.example.wallet.entity;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(
    name = "wallet_transaction",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_wallet_transaction_wallet_idempotency_key",
            columnNames = {"wallet_id", "idempotency_key"}),
    indexes =
        @Index(
            name = "idx_wallet_transaction_wallet_created",
            columnList = "wallet_id, created_at"))
public class WalletTransaction {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "wallet_id", nullable = false)
  private UUID walletId;

  @ManyToOne(fetch = LAZY, optional = false)
  @JoinColumn(
      name = "wallet_id",
      insertable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_wallet_transaction_wallet"))
  private Wallet wallet;

  @Enumerated(STRING)
  @Column(name = "type", nullable = false, length = 20)
  private TransactionType type;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
  private BigDecimal balanceAfter;

  @Column(name = "idempotency_key", nullable = false)
  private UUID idempotencyKey;

  @Column(name = "peer_wallet_id")
  private UUID peerWalletId;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(
      name = "peer_wallet_id",
      insertable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_wallet_transaction_peer_wallet"))
  private Wallet peerWallet;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected WalletTransaction() {}

  private WalletTransaction(Builder builder) {
    this.walletId = builder.walletId;
    this.type = builder.type;
    this.amount = builder.amount;
    this.balanceAfter = builder.balanceAfter;
    this.idempotencyKey = builder.idempotencyKey;
    this.peerWalletId = builder.peerWalletId;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private UUID walletId;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private UUID idempotencyKey;
    private UUID peerWalletId;

    private Builder() {}

    public Builder walletId(UUID walletId) {
      this.walletId = walletId;
      return this;
    }

    public Builder type(TransactionType type) {
      this.type = type;
      return this;
    }

    public Builder amount(BigDecimal amount) {
      this.amount = amount;
      return this;
    }

    public Builder balanceAfter(BigDecimal balanceAfter) {
      this.balanceAfter = balanceAfter;
      return this;
    }

    public Builder idempotencyKey(UUID idempotencyKey) {
      this.idempotencyKey = idempotencyKey;
      return this;
    }

    public Builder peerWalletId(UUID peerWalletId) {
      this.peerWalletId = peerWalletId;
      return this;
    }

    public WalletTransaction build() {
      return new WalletTransaction(this);
    }
  }
}
