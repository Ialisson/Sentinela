package com.sentinela.strategy;

import com.sentinela.dto.TransactionRequest;
import org.springframework.stereotype.Component;

@Component
public class ForeignCountryStrategy implements RiskStrategy {

    @Override
    public int calculate(TransactionRequest request) {
        return !"BR".equalsIgnoreCase(request.country()) ? 20 : 0;
    }
}