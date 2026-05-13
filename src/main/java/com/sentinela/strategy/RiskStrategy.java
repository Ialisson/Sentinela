package com.sentinela.strategy;

import com.sentinela.dto.TransactionRequest;

public interface RiskStrategy {
    int calculate(TransactionRequest request);
}