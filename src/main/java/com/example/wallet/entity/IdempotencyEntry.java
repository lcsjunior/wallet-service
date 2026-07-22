package com.example.wallet.entity;

import static jakarta.persistence.EnumType.STRING;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "idempotency_entry")
public class IdempotencyEntry implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "correlation_id", nullable = false)
  private String correlationId;

  @Enumerated(STRING)
  @Column(name = "operation_type", nullable = false, length = 20)
  private OperationType operationType;

  @Column(name = "request_fingerprint", nullable = false)
  private String requestFingerprint;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected IdempotencyEntry() {}

  private IdempotencyEntry(
      String correlationId, OperationType operationType, String requestFingerprint) {
    this.correlationId = correlationId;
    this.operationType = operationType;
    this.requestFingerprint = requestFingerprint;
  }

  public static IdempotencyEntry of(
      String correlationId, OperationType operationType, String requestFingerprint) {
    return new IdempotencyEntry(correlationId, operationType, requestFingerprint);
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getRequestFingerprint() {
    return requestFingerprint;
  }
}
