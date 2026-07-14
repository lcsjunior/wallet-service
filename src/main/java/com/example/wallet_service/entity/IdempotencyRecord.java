package com.example.wallet_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

    @Id
    @Column(name = "correlation_id", nullable = false, updatable = false)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, updatable = false, length = 20)
    private OperationType operationType;

    @Column(name = "request_fingerprint", nullable = false, updatable = false)
    private String requestFingerprint;

    @Lob
    @Column(name = "result_body", nullable = false, updatable = false)
    private String resultBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    private IdempotencyRecord(String correlationId, OperationType operationType, String requestFingerprint,
            String resultBody, Instant createdAt) {
        this.correlationId = correlationId;
        this.operationType = operationType;
        this.requestFingerprint = requestFingerprint;
        this.resultBody = resultBody;
        this.createdAt = createdAt;
    }

    public static IdempotencyRecord create(String correlationId, OperationType operationType,
            String requestFingerprint, String resultBody) {
        return new IdempotencyRecord(correlationId, operationType, requestFingerprint, resultBody, Instant.now());
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
