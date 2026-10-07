package com.sentinela.dto;

import com.sentinela.persistence.TransactionStatus;

public record TransactionAccepted(String transactionId, TransactionStatus status) {
}
