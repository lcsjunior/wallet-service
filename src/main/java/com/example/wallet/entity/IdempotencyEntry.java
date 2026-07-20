package com.example.wallet.entity;

import static jakarta.persistence.EnumType.STRING;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "idempotency_entry")
public class IdempotencyEntry implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "correlation_id", nullable = false, updatable = false)
  private String correlationId;

  @Enumerated(STRING)
  @Column(name = "operation_type", nullable = false, updatable = false, length = 20)
  private OperationType operationType;

  @Column(name = "request_fingerprint", nullable = false, updatable = false)
  private String requestFingerprint;

  @Lob
  @Column(name = "result_body", nullable = false, updatable = false)
  private String resultBody;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected IdempotencyEntry() {}

  private IdempotencyEntry(
      String correlationId,
      OperationType operationType,
      String requestFingerprint,
      String resultBody,
      Instant createdAt) {
    this.correlationId = correlationId;
    this.operationType = operationType;
    this.requestFingerprint = requestFingerprint;
    this.resultBody = resultBody;
    this.createdAt = createdAt;
  }

  public static IdempotencyEntry of(
      String correlationId,
      OperationType operationType,
      String requestFingerprint,
      String resultBody) {
    return new IdempotencyEntry(
        correlationId, operationType, requestFingerprint, resultBody, Instant.now());
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public String getRequestFingerprint() {
    return requestFingerprint;
  }

  public String getResultBody() {
    return resultBody;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
