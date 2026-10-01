package com.sentinela.strategy;

import com.sentinela.dto.TransactionRequest;

public interface RiskStrategy {
    String ruleId();

    int calculate(TransactionRequest request);
}
