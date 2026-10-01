package com.sentinela.strategy;

import com.sentinela.dto.TransactionRequest;
import org.springframework.stereotype.Component;

@Component
public class NewAccountStrategy implements RiskStrategy {

    @Override
    public String ruleId() {
        return "NEW_ACCOUNT";
    }

    @Override
    public int calculate(TransactionRequest request) {
        if (request.emailAgeDays() != null && request.emailAgeDays() < 7) {
            return 30;
        }
        return 0;
    }
}
