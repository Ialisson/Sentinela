package com.sentinela.service;

import com.sentinela.dto.RiskResponse;
import com.sentinela.dto.TransactionRequest;
import com.sentinela.strategy.RiskStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskAnalysisService {

    private final List<RiskStrategy> strategies;

    public RiskAnalysisService(List<RiskStrategy> strategies) {
        this.strategies = strategies;
    }

    public RiskResponse analyze(TransactionRequest request) {

        int score = strategies.stream()
                .mapToInt(strategy -> strategy.calculate(request))
                .sum();

        String level = score >= 70 ? "HIGH"
                : score >= 40 ? "MEDIUM"
                  : "LOW";

        String action = score >= 70 ? "BLOCK"
                : score >= 40 ? "REVIEW"
                  : "APPROVE";

        return new RiskResponse(score, level, action);
    }
}