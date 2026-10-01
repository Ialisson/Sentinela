package com.sentinela.dto;

import java.util.List;

public record RiskResponse(
        Integer riskScore,
        String riskLevel,
        String recommendedAction,
        List<String> triggeredRules
) {
}
