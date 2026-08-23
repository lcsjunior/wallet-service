package com.example.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "idempotency_entry")
public class IdempotencyEntry implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "correlation_id", nullable = false)
  private UUID correlationId;

  @Column(name = "request_fingerprint", nullable = false)
  private String requestFingerprint;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected IdempotencyEntry() {}

  private IdempotencyEntry(Builder builder) {
    this.correlationId = builder.correlationId;
    this.requestFingerprint = buildFingerprint(builder.operationType, builder.key, builder.amount);
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String buildFingerprint(
      OperationType operationType, String key, BigDecimal amount) {
    return operationType + ":" + key + ":" + amount.toPlainString();
  }

  public static final class Builder {

    private UUID correlationId;
    private OperationType operationType;
    private String key;
    private BigDecimal amount;

    private Builder() {}

    public Builder correlationId(UUID correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder operationType(OperationType operationType) {
      this.operationType = operationType;
      return this;
    }

    public Builder key(String key) {
      this.key = key;
      return this;
    }

    public Builder amount(BigDecimal amount) {
      this.amount = amount;
      return this;
    }

    public IdempotencyEntry build() {
      return new IdempotencyEntry(this);
    }
  }

  public UUID getCorrelationId() {
    return correlationId;
  }

  public String getRequestFingerprint() {
    return requestFingerprint;
  }
}
