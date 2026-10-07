package com.sentinela.strategy;

import com.sentinela.dto.TransactionRequest;
import com.sentinela.service.SuspiciousIpService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@Profile("worker")
@ConditionalOnProperty(name = "sentinela.redis.enabled", havingValue = "true", matchIfMissing = true)
public class SuspiciousIpStrategy implements RiskStrategy {

    private final SuspiciousIpService suspiciousIpService;

    public SuspiciousIpStrategy(SuspiciousIpService suspiciousIpService) {
        this.suspiciousIpService = suspiciousIpService;
    }

    @Override
    public String ruleId() {
        return "SUSPICIOUS_IP";
    }

    @Override
    public int calculate(TransactionRequest request) {
        return suspiciousIpService.isSuspicious(request.ipAddress()) ? 50 : 0;
    }
}
