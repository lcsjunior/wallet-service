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

  private IdempotencyEntry(UUID correlationId, String requestFingerprint) {
    this.correlationId = correlationId;
    this.requestFingerprint = requestFingerprint;
  }

  public static IdempotencyEntry of(
      UUID correlationId, OperationType operationType, String key, BigDecimal amount) {
    return new IdempotencyEntry(correlationId, buildFingerprint(operationType, key, amount));
  }

  private static String buildFingerprint(
      OperationType operationType, String key, BigDecimal amount) {
    return operationType + ":" + key + ":" + amount.toPlainString();
  }

  public UUID getCorrelationId() {
    return correlationId;
  }

  public String getRequestFingerprint() {
    return requestFingerprint;
  }
}
