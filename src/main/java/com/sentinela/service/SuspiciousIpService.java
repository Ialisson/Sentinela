package com.sentinela.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name = "sentinela.redis.enabled", havingValue = "true", matchIfMissing = true)
public class SuspiciousIpService {

    private static final Logger log = LoggerFactory.getLogger(SuspiciousIpService.class);
    private static final String SET_KEY = "sentinela:suspicious-ips";

    private final StringRedisTemplate redis;
    private final MeterRegistry meterRegistry;

    public SuspiciousIpService(StringRedisTemplate redis, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.meterRegistry = meterRegistry;
    }

    public boolean isSuspicious(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redis.opsForSet().isMember(SET_KEY, ipAddress));
        } catch (DataAccessException exception) {
            meterRegistry.counter("sentinela.redis.failures", "operation", "suspicious_ip_lookup").increment();
            log.warn("Suspicious IP lookup unavailable; continuing with the remaining risk rules.");
            return false;
        }
    }
}
