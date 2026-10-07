package com.sentinela.dto;

import com.sentinela.persistence.TransactionRecord;
import com.sentinela.persistence.TransactionStatus;

import java.time.Instant;
import java.util.List;

public record TransactionStatusResponse(
        String transactionId,
        TransactionStatus status,
        Integer riskScore,
        String riskLevel,
        String recommendedAction,
        List<String> triggeredRules,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {
    public static TransactionStatusResponse from(TransactionRecord record) {
        return new TransactionStatusResponse(record.transactionId(), record.status(), record.riskScore(),
                record.riskLevel(), record.recommendedAction(), record.triggeredRules(), record.failureReason(),
                record.createdAt(), record.completedAt());
    }
}
