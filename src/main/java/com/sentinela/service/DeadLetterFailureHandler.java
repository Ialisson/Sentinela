package com.sentinela.service;

import com.sentinela.persistence.TransactionRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("api")
public class DeadLetterFailureHandler {
    private final TransactionRecordRepository transactions;

    public DeadLetterFailureHandler(TransactionRecordRepository transactions) {
        this.transactions = transactions;
    }

    @Transactional
    public void markFailed(String transactionId) {
        transactions.findByTransactionId(transactionId).ifPresent(record ->
                record.fail("Risk analysis exhausted retries and was moved to the dead-letter queue."));
    }
}
