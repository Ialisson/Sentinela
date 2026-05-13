package com.sentinela.strategy;

import com.sentinela.dto.TransactionRequest;
import org.springframework.stereotype.Component;

@Component
public class HighAmountStrategy implements RiskStrategy {

    @Override
    public int calculate(TransactionRequest request) {
        return request.amount() > 5000 ? 40 : 0;
    }
}