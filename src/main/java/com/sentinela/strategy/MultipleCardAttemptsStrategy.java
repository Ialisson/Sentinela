package com.sentinela.strategy;

import com.sentinela.dto.TransactionRequest;
import org.springframework.stereotype.Component;

@Component
public class MultipleCardAttemptsStrategy implements RiskStrategy {

    private static final int ATTEMPT_THRESHOLD = 3;

    @Override
    public String ruleId() {
        return "MULTIPLE_CARD_ATTEMPTS";
    }

    @Override
    public int calculate(TransactionRequest request) {
        return request.cardAttempts() >= ATTEMPT_THRESHOLD ? 25 : 0;
    }
}
