package com.sentinela.persistence;

import com.sentinela.dto.RiskResponse;
import com.sentinela.dto.TransactionRequest;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_idempotency_key", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_transactions_transaction_id", columnNames = "transaction_id")
})
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "card_attempts", nullable = false)
    private Integer cardAttempts;

    @Column(name = "email_age_days")
    private Integer emailAgeDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "recommended_action", length = 20)
    private String recommendedAction;

    @Column(name = "triggered_rules", length = 500)
    private String triggeredRules;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    private long version;

    protected TransactionRecord() {
    }

    public TransactionRecord(String idempotencyKey, TransactionRequest request) {
        this.idempotencyKey = idempotencyKey;
        this.transactionId = request.transactionId();
        this.userId = request.userId();
        this.amount = request.amount();
        this.country = request.country().toUpperCase(java.util.Locale.ROOT);
        this.ipAddress = request.ipAddress();
        this.cardAttempts = request.cardAttempts();
        this.emailAgeDays = request.emailAgeDays();
        this.status = TransactionStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public TransactionRequest toRequest() {
        return new TransactionRequest(transactionId, userId, amount, country, ipAddress, cardAttempts, emailAgeDays);
    }

    public boolean hasSameRequest(TransactionRequest request) {
        return transactionId.equals(request.transactionId())
                && userId.equals(request.userId())
                && amount.compareTo(request.amount()) == 0
                && country.equalsIgnoreCase(request.country())
                && java.util.Objects.equals(ipAddress, request.ipAddress())
                && cardAttempts.equals(request.cardAttempts())
                && java.util.Objects.equals(emailAgeDays, request.emailAgeDays());
    }

    public void complete(RiskResponse response) {
        if (status != TransactionStatus.PENDING) {
            return;
        }
        status = TransactionStatus.COMPLETED;
        riskScore = response.riskScore();
        riskLevel = response.riskLevel();
        recommendedAction = response.recommendedAction();
        triggeredRules = String.join(",", response.triggeredRules());
        completedAt = Instant.now();
    }

    public void fail(String reason) {
        if (status != TransactionStatus.PENDING) return;
        status = TransactionStatus.FAILED;
        failureReason = reason == null || reason.isBlank() ? "Risk analysis failed after retries." : reason;
        completedAt = Instant.now();
    }

    public TransactionStatus status() { return status; }
    public String transactionId() { return transactionId; }
    public String idempotencyKey() { return idempotencyKey; }
    public Integer riskScore() { return riskScore; }
    public String riskLevel() { return riskLevel; }
    public String recommendedAction() { return recommendedAction; }
    public List<String> triggeredRules() {
        return triggeredRules == null || triggeredRules.isBlank()
                ? List.of()
                : Arrays.asList(triggeredRules.split(","));
    }
    public Instant createdAt() { return createdAt; }
    public Instant completedAt() { return completedAt; }
    public String failureReason() { return failureReason; }
}
