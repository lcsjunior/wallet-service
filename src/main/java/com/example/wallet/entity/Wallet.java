package com.example.wallet.entity;

import static com.example.wallet.constants.Constants.ZERO_MONEY;
import static com.example.wallet.constants.Messages.INSUFFICIENT_BALANCE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.example.wallet.exception.ServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "wallet")
public class Wallet {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
  private UUID idempotencyKey;

  @Column(name = "balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal balance = ZERO_MONEY;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected Wallet() {}

  private Wallet(UUID userId, UUID idempotencyKey) {
    this.userId = userId;
    this.idempotencyKey = idempotencyKey;
  }

  public static Wallet of(UUID userId, UUID idempotencyKey) {
    return new Wallet(userId, idempotencyKey);
  }

  public void credit(BigDecimal amount) {
    this.balance = this.balance.add(amount);
  }

  public void debit(BigDecimal amount) {
    if (this.balance.compareTo(amount) < 0) {
      throw ServiceException.of(INSUFFICIENT_BALANCE, UNPROCESSABLE_ENTITY);
    }
    this.balance = this.balance.subtract(amount);
  }

  public UUID getId() {
    return id;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
