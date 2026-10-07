package com.sentinela.service;

import com.sentinela.persistence.TransactionRecord;
import com.sentinela.persistence.TransactionRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("worker")
public class RiskMessageProcessor {

    private final TransactionRecordRepository transactions;
    private final RiskAnalysisService riskAnalysisService;

    public RiskMessageProcessor(TransactionRecordRepository transactions, RiskAnalysisService riskAnalysisService) {
        this.transactions = transactions;
        this.riskAnalysisService = riskAnalysisService;
    }

    @Transactional
    public boolean process(String transactionId) {
        TransactionRecord record = transactions.lockByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction does not exist: " + transactionId));
        if (record.status() != com.sentinela.persistence.TransactionStatus.PENDING) {
            return false;
        }
        record.complete(riskAnalysisService.analyze(record.toRequest()));
        return true;
    }
}
