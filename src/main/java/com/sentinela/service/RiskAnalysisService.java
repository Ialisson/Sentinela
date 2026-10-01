package com.sentinela.service;

import com.sentinela.dto.RiskResponse;
import com.sentinela.dto.TransactionRequest;
import com.sentinela.strategy.RiskStrategy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RiskAnalysisService {

    private final List<RiskStrategy> strategies;

    public RiskAnalysisService(List<RiskStrategy> strategies) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparing(RiskStrategy::ruleId))
                .toList();
    }

    public RiskResponse analyze(TransactionRequest request) {

        int rawScore = 0;
        List<String> triggeredRules = new ArrayList<>();
        for (RiskStrategy strategy : strategies) {
            int ruleScore = strategy.calculate(request);
            rawScore = Math.min(100, rawScore + ruleScore);
            if (ruleScore > 0) {
                triggeredRules.add(strategy.ruleId());
            }
        }
        int score = rawScore;

        String level = score >= 70 ? "HIGH"
                : score >= 40 ? "MEDIUM"
                  : "LOW";

        String action = score >= 70 ? "BLOCK"
                : score >= 40 ? "REVIEW"
                  : "APPROVE";

        return new RiskResponse(score, level, action, triggeredRules);
    }
}
