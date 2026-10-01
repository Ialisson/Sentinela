package com.sentinela.strategy;

import com.sentinela.dto.TransactionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HighAmountStrategy implements RiskStrategy {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("5000.00");

    @Override
    public String ruleId() {
        return "HIGH_AMOUNT";
    }

    @Override
    public int calculate(TransactionRequest request) {
        return request.amount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0 ? 40 : 0;
    }
}
