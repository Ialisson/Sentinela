package com.sentinela.dto;

public record RiskResponse(
        Integer riskScore,
        String riskLevel,
        String recommendedAction
) {
}