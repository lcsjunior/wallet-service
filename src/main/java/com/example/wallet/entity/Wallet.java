package com.example.wallet.entity;

import static java.math.BigDecimal.ZERO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet")
public class Wallet {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected Wallet() {}

  private Wallet(UUID id, UUID userId, BigDecimal balance, Instant createdAt) {
    this.id = id;
    this.userId = userId;
    this.balance = balance;
    this.createdAt = createdAt;
  }

  public static Wallet of(UUID userId) {
    return new Wallet(UUID.randomUUID(), userId, ZERO.setScale(2), Instant.now());
  }

  public void credit(BigDecimal amount) {
    this.balance = this.balance.add(amount);
  }

  public void debit(BigDecimal amount) {
    this.balance = this.balance.subtract(amount);
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public long getVersion() {
    return version;
  }
}
