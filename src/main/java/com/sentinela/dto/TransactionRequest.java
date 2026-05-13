package com.sentinela.dto;

public record TransactionRequest(
        String transactionId,
        Double amount,
        String country,
        String ipAddress,
        Integer emailAgeDays
) {
}